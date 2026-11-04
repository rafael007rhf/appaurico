// Explicit integration test: uses a real local Keycloak, never a production realm.
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { readFile } from "node:fs/promises";
import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { buildApp } from "../src/app.js";
import type { Database } from "../src/db.js";
const issuer = process.env.OIDC_ISSUER!;
assert.equal(
  issuer,
  "http://127.0.0.1:8080/realms/aurico",
  "Test requires the dedicated local realm",
);
const base = "http://127.0.0.1:8080";
const tokenResponse = await fetch(
  `${base}/realms/master/protocol/openid-connect/token`,
  {
    method: "POST",
    body: new URLSearchParams({
      grant_type: "password",
      client_id: "admin-cli",
      username: "admin",
      password: process.env.KEYCLOAK_ADMIN_PASSWORD!,
    }),
  },
);
assert.equal(
  tokenResponse.status,
  200,
  "Local test admin authentication failed",
);
const adminToken = (await tokenResponse.json()).access_token;
const suffix = randomBytes(8).toString("hex"),
  username = `oidc-test-${suffix}`,
  password = randomBytes(24).toString("hex");
const headers = {
  Authorization: `Bearer ${adminToken}`,
  "Content-Type": "application/json",
};
const created = await fetch(`${base}/admin/realms/aurico/users`, {
  method: "POST",
  headers,
  body: JSON.stringify({
    username,
    email: `${username}@example.invalid`,
    emailVerified: true,
    enabled: true,
    firstName: "Integration",
    lastName: "Test",
    credentials: [{ type: "password", value: password, temporary: false }],
  }),
});
assert.equal(created.status, 201, "Test identity creation failed");
const userLocation = created.headers.get("location")!;
assert.equal(new URL(userLocation).origin, base);
const pg = new PGlite(),
  orm = drizzle(pg);
const wrap = (db: Pick<typeof orm, "execute" | "transaction">): Database => ({
  query: async <T>(q) => (await db.execute(q)).rows as T[],
  transaction: (run) => db.transaction((tx) => run(wrap(tx))),
  close: () => pg.close(),
});
await pg.exec(
  await readFile(
    new URL("../migrations/0001_platform.sql", import.meta.url),
    "utf8",
  ),
);
await pg.exec(
  await readFile(
    new URL("../migrations/0002_governance.sql", import.meta.url),
    "utf8",
  ),
);
const app = await buildApp(wrap(orm), {
  issuer,
  audience: "aurico-api",
  clientId: "aurico-web",
  clientSecret: process.env.OIDC_CLIENT_SECRET!,
  webOrigin: "http://127.0.0.1:3000",
  allowLocalHttp: true,
});
try {
  const login = await app.inject("/api/v1/auth/login");
  assert.equal(login.statusCode, 302, "Login initiation failed");
  const flowCookie = login.cookies.find((c) => c.name === "aurico_flow")!;
  assert.ok(flowCookie);
  const jar = new Map<string, string>();
  function absorb(response: Response) {
    for (const value of response.headers.getSetCookie()) {
      const part = value.split(";")[0]!,
        index = part.indexOf("=");
      jar.set(part.slice(0, index), part.slice(index + 1));
    }
  }
  const form = await fetch(login.headers.location!, { redirect: "manual" });
  absorb(form);
  assert.equal(form.status, 200, "Provider did not show login");
  const html = await form.text(),
    action = html
      .match(/<form[^>]*action="([^"]+)"/)?.[1]
      ?.replaceAll("&amp;", "&");
  assert.ok(action, "Provider login form missing");
  assert.equal(new URL(action!).origin, base);
  const authorized = await fetch(action!, {
    method: "POST",
    redirect: "manual",
    headers: { Cookie: [...jar].map(([k, v]) => `${k}=${v}`).join("; ") },
    body: new URLSearchParams({ username, password, credentialId: "" }),
  });
  assert.equal(authorized.status, 302, "Provider authorization failed");
  const target = new URL(authorized.headers.get("location")!);
  assert.equal(target.origin, "http://127.0.0.1:3000");
  const callback = await app.inject({
    url: target.pathname + target.search,
    headers: { cookie: `aurico_flow=${flowCookie.value}` },
  });
  assert.equal(callback.statusCode, 302, "OIDC callback failed");
  const session = callback.cookies.find((c) => c.name === "aurico_session")!;
  assert.ok(session, "Session not established");
  const me = await app.inject({
    url: "/api/v1/me",
    headers: { cookie: `aurico_session=${session.value}` },
  });
  assert.equal(me.statusCode, 200);
  assert.equal(me.json().role, "patient");
  const logout = await app.inject({
    method: "POST",
    url: "/api/v1/auth/logout",
    headers: {
      cookie: `aurico_session=${session.value}`,
      origin: "http://127.0.0.1:3000",
    },
  });
  assert.equal(logout.statusCode, 200);
  assert.equal(
    (
      await app.inject({
        url: "/api/v1/me",
        headers: { cookie: `aurico_session=${session.value}` },
      })
    ).statusCode,
    401,
  );
  console.log(
    "PASS: real Keycloak discovery, PKCE authorization code, state/nonce, JWKS validation, session and logout.",
  );
} finally {
  await app.close();
  await pg.close();
  await fetch(userLocation, { method: "DELETE", headers });
}
