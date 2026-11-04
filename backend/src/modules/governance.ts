import { randomUUID } from "node:crypto";
import { z } from "zod";
import type { FastifyInstance } from "fastify";
import { sql, type Database } from "../db.js";
import { actorOf } from "./auth.js";
import { ApiError, audit, idParams } from "./common.js";
export function registerGovernance(app: FastifyInstance, db: Database) {
  app.get("/api/v1/governance/requests", async (req) => {
    const actor = actorOf(req);
    return {
      items: await db.query(
        sql`SELECT id,kind,status,details,decision,created_at,updated_at,version FROM governance_request WHERE user_id=${actor.id} ORDER BY created_at DESC LIMIT 100`,
      ),
    };
  });
  app.post("/api/v1/governance/requests", async (req, reply) => {
    const actor = actorOf(req),
      body = z
        .discriminatedUnion("kind", [
          z
            .object({ kind: z.literal("deletion"), confirmed: z.literal(true) })
            .strict(),
          z
            .object({
              kind: z.literal("professional_verification"),
              registration: z.string().trim().min(3).max(120),
              jurisdiction: z.string().trim().min(2).max(120),
              evidenceReference: z.string().trim().min(3).max(500),
            })
            .strict(),
          z
            .object({
              kind: z.literal("guardian_review"),
              relationship: z.string().trim().min(2).max(80),
              evidenceReference: z.string().trim().min(3).max(500),
              confirmed: z.literal(true),
            })
            .strict(),
        ])
        .parse(req.body);
    if (
      body.kind === "professional_verification" &&
      actor.role !== "professional"
    )
      throw new ApiError(403, "PROFESSIONAL_REQUIRED");
    const id = randomUUID();
    await db.transaction(async (tx) => {
      await tx.query(
        sql`INSERT INTO governance_request(id,user_id,kind,details) VALUES(${id},${actor.id},${body.kind},${JSON.stringify(body)}::jsonb)`,
      );
      await audit(tx, actor, "governance.request", id);
    });
    return reply.code(201).send({ id, status: "requested" });
  });
  app.post("/api/v1/governance/requests/:id/cancel", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    return db.transaction(async (tx) => {
      const rows = await tx.query(
        sql`UPDATE governance_request SET status='cancelled',version=version+1,updated_at=now() WHERE id=${id} AND user_id=${actor.id} AND status IN ('requested','needs_information') RETURNING id`,
      );
      if (!rows.length) throw new ApiError(404, "RESOURCE_NOT_FOUND");
      await audit(tx, actor, "governance.cancel", id);
      return { ok: true };
    });
  });
  app.get("/api/v1/operations/requests", async (req) => {
    const actor = actorOf(req);
    if (!actor.operations_access)
      throw new ApiError(403, "OPERATIONS_REQUIRED");
    await audit(db, actor, "governance.queue.read");
    return {
      items: await db.query(
        sql`SELECT r.*,u.display_name FROM governance_request r JOIN app_user u ON u.id=r.user_id ORDER BY r.created_at LIMIT 100`,
      ),
    };
  });
  app.post("/api/v1/operations/requests/:id/review", async (req) => {
    const actor = actorOf(req);
    if (!actor.operations_access)
      throw new ApiError(403, "OPERATIONS_REQUIRED");
    const { id } = idParams.parse(req.params);
    const body = z
      .object({
        version: z.number().int().positive(),
        status: z.enum([
          "under_review",
          "needs_information",
          "approved",
          "denied",
        ]),
        decision: z.string().trim().min(10).max(2000),
        evidenceChecked: z.literal(true),
      })
      .strict()
      .parse(req.body);
    return db.transaction(async (tx) => {
      const [request] = await tx.query<{
        user_id: string;
        kind: string;
        status: string;
        version: number;
      }>(sql`SELECT * FROM governance_request WHERE id=${id} FOR UPDATE`);
      if (!request) throw new ApiError(404, "RESOURCE_NOT_FOUND");
      if (request.user_id === actor.id)
        throw new ApiError(403, "SELF_REVIEW_FORBIDDEN");
      if (
        request.version !== body.version ||
        ["approved", "denied", "cancelled"].includes(request.status)
      )
        throw new ApiError(409, "VERSION_CONFLICT");
      // Deletion and guardian authority require an approved external process, not an API status flip.
      if (
        body.status === "approved" &&
        request.kind !== "professional_verification"
      )
        throw new ApiError(409, "EXTERNAL_REVIEW_REQUIRED");
      await tx.query(
        sql`UPDATE governance_request SET status=${body.status},decision=${body.decision},reviewer_id=${actor.id},version=version+1,updated_at=now() WHERE id=${id}`,
      );
      if (
        request.kind === "professional_verification" &&
        body.status === "approved"
      )
        await tx.query(
          sql`UPDATE professional_profile SET verification_status='verified' WHERE user_id=${request.user_id}`,
        );
      await audit(tx, actor, "governance.review", id);
      return { ok: true, version: body.version + 1 };
    });
  });
}
