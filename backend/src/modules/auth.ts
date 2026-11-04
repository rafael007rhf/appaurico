import { createHash, randomBytes, randomUUID } from "node:crypto";
import * as oidc from "openid-client";
import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";
import type { FastifyInstance, FastifyRequest } from "fastify";
import { sql, type Database } from "../db.js";
import { ApiError, type Actor } from "./common.js";
export const hash = (value: string) =>
  createHash("sha256").update(value).digest("hex");
export type AuthConfig = {
  issuer: string;
  audience: string;
  clientId: string;
  clientSecret: string;
  webOrigin: string;
  allowLocalHttp: boolean;
};
export function tokenVerifier(config: AuthConfig) {
  const keys = createRemoteJWKSet(
    new URL(`${config.issuer}/protocol/openid-connect/certs`),
  );
  return async (token: string): Promise<JWTPayload> =>
    (
      await jwtVerify(token, keys, {
        issuer: config.issuer,
        audience: config.audience,
        algorithms: ["RS256"],
        requiredClaims: ["sub", "exp", "iat"],
      })
    ).payload;
}
export async function provision(
  db: Database,
  claims: JWTPayload,
  issuer: string,
): Promise<Actor> {
  const realm = claims.realm_access as { roles?: string[] } | undefined;
  const role = realm?.roles?.includes("professional")
    ? "professional"
    : realm?.roles?.includes("patient")
      ? "patient"
      : null;
  if (!role || !claims.sub || claims.iss !== issuer)
    throw new ApiError(403, "ACCOUNT_ROLE_REQUIRED");
  return db.transaction(async (tx) => {
    const [user] = await tx.query<Actor>(
      sql`INSERT INTO app_user(id,issuer,subject,role,display_name,email,email_verified) VALUES(${randomUUID()},${issuer},${claims.sub},${role},${typeof claims.name === "string" ? claims.name.slice(0, 160) : "Usuário"},${typeof claims.email === "string" ? claims.email.toLowerCase() : null},${claims.email_verified === true}) ON CONFLICT(issuer,subject) DO UPDATE SET email=EXCLUDED.email,email_verified=EXCLUDED.email_verified,display_name=EXCLUDED.display_name,updated_at=now() RETURNING *`,
    );
    // Role changes require an explicit account migration; never silently expose another role's data.
    if (user!.role !== role) throw new ApiError(403, "ACCOUNT_ROLE_CHANGED");
    if (role === "patient")
      await tx.query(
        sql`INSERT INTO patient_profile(user_id) VALUES(${user!.id}) ON CONFLICT DO NOTHING`,
      );
    else
      await tx.query(
        sql`INSERT INTO professional_profile(user_id) VALUES(${user!.id}) ON CONFLICT DO NOTHING`,
      );
    return user!;
  });
}
export async function registerAuth(
  app: FastifyInstance,
  db: Database,
  config: AuthConfig,
  verify = tokenVerifier(config),
) {
  const secure = !config.allowLocalHttp;
  const cookie = {
    httpOnly: true,
    secure,
    sameSite: "lax" as const,
    path: "/api/v1",
    maxAge: 3600,
  };
  let discovery: Promise<oidc.Configuration> | undefined;
  const getConfig = () =>
    (discovery ??= oidc.discovery(
      new URL(config.issuer),
      config.clientId,
      config.clientSecret,
      undefined,
      config.allowLocalHttp
        ? { execute: [oidc.allowInsecureRequests] }
        : undefined,
    ));
  app.decorateRequest("actor", null);
  app.addHook("preHandler", async (req) => {
    if (req.routeOptions.config.public) return;
    const authorization = req.headers.authorization;
    if (authorization) {
      if (!authorization.startsWith("Bearer "))
        throw new ApiError(401, "UNAUTHENTICATED");
      try {
        req.actor = await provision(
          db,
          await verify(authorization.slice(7)),
          config.issuer,
        );
      } catch (e) {
        if (e instanceof ApiError) throw e;
        throw new ApiError(401, "UNAUTHENTICATED");
      }
    } else {
      const token = req.cookies.aurico_session;
      if (!token) throw new ApiError(401, "UNAUTHENTICATED");
      const [user] = await db.query<Actor>(
        sql`SELECT u.* FROM app_user u JOIN web_session s ON s.user_id=u.id WHERE s.token_hash=${hash(token)} AND s.expires_at>now()`,
      );
      if (!user) throw new ApiError(401, "UNAUTHENTICATED");
      if (
        !["GET", "HEAD", "OPTIONS"].includes(req.method) &&
        req.headers.origin !== config.webOrigin
      )
        throw new ApiError(403, "CSRF_REJECTED");
      req.actor = user;
    }
  });
  app.get(
    "/api/v1/auth/login",
    { config: { public: true } },
    async (_req, reply) => {
      const client = await getConfig();
      const state = oidc.randomState(),
        nonce = oidc.randomNonce(),
        verifier = oidc.randomPKCECodeVerifier();
      await db.query(sql`DELETE FROM auth_flow WHERE expires_at<now()`);
      await db.query(
        sql`INSERT INTO auth_flow(state_hash,verifier,nonce,expires_at) VALUES(${hash(state)},${verifier},${nonce},now()+interval '5 minutes')`,
      );
      reply.setCookie("aurico_flow", state, { ...cookie, maxAge: 300 });
      return reply.redirect(
        oidc.buildAuthorizationUrl(client, {
          redirect_uri: `${config.webOrigin}/api/v1/auth/callback`,
          scope: "openid profile email",
          code_challenge: await oidc.calculatePKCECodeChallenge(verifier),
          code_challenge_method: "S256",
          state,
          nonce,
        }).href,
      );
    },
  );
  app.get(
    "/api/v1/auth/callback",
    { config: { public: true } },
    async (req, reply) => {
      const state = req.cookies.aurico_flow;
      if (!state) throw new ApiError(401, "LOGIN_EXPIRED");
      const [flow] = await db.query<{ verifier: string; nonce: string }>(
        sql`DELETE FROM auth_flow WHERE state_hash=${hash(state)} AND expires_at>now() RETURNING verifier,nonce`,
      );
      reply.clearCookie("aurico_flow", { path: "/api/v1" });
      if (!flow) throw new ApiError(401, "LOGIN_EXPIRED");
      const tokens = await oidc.authorizationCodeGrant(
        await getConfig(),
        new URL(req.url, config.webOrigin),
        {
          pkceCodeVerifier: flow.verifier,
          expectedState: state,
          expectedNonce: flow.nonce,
        },
      );
      const actor = await provision(
        db,
        await verify(tokens.access_token),
        config.issuer,
      );
      const token = randomBytes(32).toString("base64url");
      await db.query(
        sql`INSERT INTO web_session(token_hash,user_id,expires_at) VALUES(${hash(token)},${actor.id},now()+interval '1 hour')`,
      );
      reply.setCookie("aurico_session", token, cookie);
      return reply.redirect("/");
    },
  );
  app.post("/api/v1/auth/logout", async (req, reply) => {
    if (req.cookies.aurico_session)
      await db.query(
        sql`DELETE FROM web_session WHERE token_hash=${hash(req.cookies.aurico_session)}`,
      );
    reply.clearCookie("aurico_session", { path: "/api/v1" });
    return { ok: true };
  });
  app.get("/api/v1/me", async (req) => {
    const profile =
      req.actor!.role === "professional"
        ? (
            await db.query(
              sql`SELECT verification_status,registration FROM professional_profile WHERE user_id=${req.actor!.id}`,
            )
          )[0]
        : null;
    return { ...req.actor, profile };
  });
}
export const actorOf = (req: FastifyRequest) => {
  if (!req.actor) throw new ApiError(401, "UNAUTHENTICATED");
  return req.actor;
};
declare module "fastify" {
  interface FastifyRequest {
    actor: Actor | null;
  }
  interface FastifyContextConfig {
    public?: boolean;
  }
}
