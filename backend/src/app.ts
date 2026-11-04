import Fastify from "fastify";
import { openapi } from "./openapi.js";
import cookie from "@fastify/cookie";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import { ZodError } from "zod";
import type { JWTPayload } from "jose";
import type { Database } from "./db.js";
import { ApiError } from "./modules/common.js";
import { registerAuth, type AuthConfig } from "./modules/auth.js";
import { registerPatients } from "./modules/patients.js";
import { registerTreatments } from "./modules/treatments.js";
import { registerRecords } from "./modules/records.js";
import { registerReporting } from "./modules/reporting.js";
export async function buildApp(
  db: Database,
  config: AuthConfig,
  verify?: (token: string) => Promise<JWTPayload>,
) {
  const app = Fastify({
    logger: {
      level: "info",
      redact: [
        "req.headers.authorization",
        "req.headers.cookie",
        "res.headers.set-cookie",
      ],
    },
    disableRequestLogging: true,
    bodyLimit: 256 * 1024,
    trustProxy: false,
  });
  await app.register(cookie);
  await app.register(helmet);
  await app.register(rateLimit, { max: 120, timeWindow: "1 minute" });
  app.addHook("onSend", async (_req, reply) => {
    reply.header("Cache-Control", "no-store").header("Pragma", "no-cache");
  });
  app.setErrorHandler((error, req, reply) => {
    if (error instanceof ZodError)
      return reply
        .code(422)
        .send({ error: { code: "VALIDATION_ERROR", requestId: req.id } });
    if (error instanceof ApiError)
      return reply
        .code(error.status)
        .send({ error: { code: error.code, requestId: req.id } });
    const e = error as {
      code?: string;
      statusCode?: number;
      cause?: { code?: string };
    };
    if (["23505", "23503", "23514"].includes(e.code ?? e.cause?.code ?? ""))
      return reply
        .code(409)
        .send({ error: { code: "RESOURCE_CONFLICT", requestId: req.id } });
    if (e.statusCode && e.statusCode < 500)
      return reply
        .code(e.statusCode)
        .send({
          error: {
            code: e.statusCode === 429 ? "RATE_LIMITED" : "INVALID_REQUEST",
            requestId: req.id,
          },
        });
    req.log.error(
      { requestId: req.id, code: "INTERNAL_ERROR" },
      "Request failed",
    );
    return reply
      .code(500)
      .send({ error: { code: "INTERNAL_ERROR", requestId: req.id } });
  });
  app.get("/api/v1/openapi", { config: { public: true } }, async () => openapi);
  app.get("/health", { config: { public: true } }, async () => ({
    status: "ok",
  }));
  await registerAuth(app, db, config, verify);
  registerPatients(app, db);
  registerTreatments(app, db);
  registerRecords(app, db);
  registerReporting(app, db);
  return app;
}
