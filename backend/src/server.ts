import { z } from "zod";
import { buildApp } from "./app.js";
import { connect } from "./db.js";
const env = z
  .object({
    DATABASE_URL: z.url(),
    OIDC_ISSUER: z.url(),
    OIDC_AUDIENCE: z.string().min(1),
    OIDC_CLIENT_ID: z.string().min(1),
    OIDC_CLIENT_SECRET: z.string().min(1),
    WEB_ORIGIN: z.url(),
    ALLOW_LOCAL_HTTP: z.enum(["true", "false"]).default("false"),
    PORT: z.coerce.number().default(4000),
  })
  .parse(process.env);
const local = env.ALLOW_LOCAL_HTTP === "true";
for (const value of [env.OIDC_ISSUER, env.WEB_ORIGIN]) {
  const u = new URL(value);
  if (
    u.protocol !== "https:" &&
    !(local && ["localhost", "127.0.0.1"].includes(u.hostname))
  )
    throw new Error("HTTPS is required outside loopback development.");
}
if (process.env.NODE_ENV === "production" && local)
  throw new Error("Local HTTP is forbidden in production.");
const db = connect(env.DATABASE_URL),
  app = await buildApp(db, {
    issuer: env.OIDC_ISSUER,
    audience: env.OIDC_AUDIENCE,
    clientId: env.OIDC_CLIENT_ID,
    clientSecret: env.OIDC_CLIENT_SECRET,
    webOrigin: env.WEB_ORIGIN,
    allowLocalHttp: local,
  });
app.addHook("onClose", () => db.close());
await app.listen({ port: env.PORT, host: "127.0.0.1" });
for (const signal of ["SIGINT", "SIGTERM"])
  process.on(signal, () => void app.close());
