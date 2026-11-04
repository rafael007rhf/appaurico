import { randomBytes, randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { sql, type Database } from "../db.js";
import { actorOf, hash } from "./auth.js";
import {
  ApiError,
  audit,
  change,
  idParams,
  lockPatient,
  pagination,
  patient,
  professional,
} from "./common.js";
export function registerPatients(app: FastifyInstance, db: Database) {
  app.get("/api/v1/patients", async (req) => {
    const actor = actorOf(req);
    professional(actor);
    const { page, limit, q } = pagination.parse(req.query);
    await audit(db, actor, "patients.list");
    return {
      items: await db.query(
        sql`SELECT u.id,u.display_name,l.id AS link_id,l.status FROM app_user u JOIN patient_professional_link l ON l.patient_id=u.id WHERE l.professional_id=${actor.id} AND l.status='active' AND u.display_name ILIKE ${"%" + q + "%"} ORDER BY u.display_name,u.id LIMIT ${limit} OFFSET ${(page - 1) * limit}`,
      ),
      page,
      limit,
    };
  });
  app.get("/api/v1/patient-links", async (req) => {
    const actor = actorOf(req);
    patient(actor);
    return {
      items: await db.query(
        sql`SELECT l.id,l.status,u.display_name,p.verification_status FROM patient_professional_link l JOIN app_user u ON u.id=l.professional_id JOIN professional_profile p ON p.user_id=u.id WHERE l.patient_id=${actor.id} ORDER BY l.created_at DESC LIMIT 100`,
      ),
    };
  });
  app.post("/api/v1/patient-invitations", async (req, reply) => {
    const actor = actorOf(req);
    professional(actor);
    const { email } = z
      .object({ email: z.email().max(254) })
      .strict()
      .parse(req.body);
    const token = randomBytes(32).toString("base64url"),
      id = randomUUID();
    await db.transaction(async (tx) => {
      await tx.query(
        sql`INSERT INTO patient_invitation(id,professional_id,email,token_hash,expires_at) VALUES(${id},${actor.id},${email.toLowerCase()},${hash(token)},now()+interval '7 days')`,
      );
      await audit(tx, actor, "invitation.create", id);
    });
    return reply.code(201).send({ id, token, expiresInDays: 7 });
  });
  app.post("/api/v1/patient-links/accept", async (req) => {
    const actor = actorOf(req);
    patient(actor);
    const { token } = z
      .object({
        token: z.string().min(40).max(100),
        consent: z.literal(true),
        policyVersion: z.literal("2026-11-03"),
      })
      .strict()
      .parse(req.body);
    if (!actor.email_verified || !actor.email)
      throw new ApiError(403, "VERIFIED_EMAIL_REQUIRED");
    return db.transaction(async (tx) => {
      await lockPatient(tx, actor.id);
      const [invite] = await tx.query<{
        id: string;
        professional_id: string;
        accepted_by: string | null;
      }>(
        sql`SELECT * FROM patient_invitation WHERE token_hash=${hash(token)} AND email=${actor.email} AND expires_at>now() FOR UPDATE`,
      );
      if (!invite || invite.accepted_by)
        throw new ApiError(404, "INVITATION_UNAVAILABLE");
      const [link] = await tx.query<{ id: string }>(
        sql`INSERT INTO patient_professional_link(id,patient_id,professional_id) VALUES(${randomUUID()},${actor.id},${invite.professional_id}) ON CONFLICT(patient_id,professional_id) DO UPDATE SET status='active',version=patient_professional_link.version+1,updated_at=now() RETURNING id`,
      );
      await tx.query(
        sql`UPDATE patient_invitation SET accepted_by=${actor.id} WHERE id=${invite.id}`,
      );
      await tx.query(
        sql`INSERT INTO consent_record(id,link_id,patient_id,policy_version,action) VALUES(${randomUUID()},${link!.id},${actor.id},'2026-11-03','accepted')`,
      );
      await audit(tx, actor, "link.accept", link!.id);
      await change(tx, actor.id, "link", link!.id, {
        id: link!.id,
        status: "active",
        professionalId: invite.professional_id,
      });
      return { id: link!.id };
    });
  });
  app.delete("/api/v1/patient-links/:id", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    return db.transaction(async (tx) => {
      const [link] = await tx.query<{
        patient_id: string;
        professional_id: string;
      }>(
        sql`SELECT patient_id,professional_id FROM patient_professional_link WHERE id=${id}`,
      );
      if (!link || ![link.patient_id, link.professional_id].includes(actor.id))
        throw new ApiError(404, "RESOURCE_NOT_FOUND");
      await lockPatient(tx, link.patient_id);
      await tx.query(
        sql`UPDATE patient_professional_link SET status='revoked',version=version+1,updated_at=now() WHERE id=${id}`,
      );
      await tx.query(
        sql`INSERT INTO consent_record(id,link_id,patient_id,policy_version,action) VALUES(${randomUUID()},${id},${link.patient_id},'2026-11-03','revoked')`,
      );
      await audit(tx, actor, "link.revoke", id);
      await change(tx, link.patient_id, "link", id, {
        id,
        status: "revoked",
        professionalId: link.professional_id,
      });
      return { ok: true };
    });
  });
}
