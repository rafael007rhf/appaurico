import { randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { sql, type Database } from "../db.js";
import { actorOf } from "./auth.js";
import {
  ApiError,
  accessTreatment,
  activeLink,
  audit,
  change,
  idParams,
  instant,
  lockPatient,
  pagination,
  professional,
  uuid,
  type Actor,
  type Treatment,
} from "./common.js";
import { planInput, schedule, type Plan } from "./plans.js";
export async function snapshot(db: Database, id: string) {
  const [treatment] = await db.query(
    sql`SELECT * FROM treatment WHERE id=${id}`,
  );
  const [plan] = await db.query(
    sql`SELECT * FROM treatment_plan_version WHERE treatment_id=${id} ORDER BY version DESC LIMIT 1`,
  );
  const symptoms = await db.query(
    sql`SELECT * FROM treatment_symptom WHERE treatment_id=${id} ORDER BY name`,
  );
  const occurrences = await db.query(
    sql`SELECT * FROM scheduled_stimulation WHERE treatment_id=${id} ORDER BY scheduled_at`,
  );
  const appointments = await db.query(
    sql`SELECT * FROM follow_up_appointment WHERE treatment_id=${id} ORDER BY scheduled_at`,
  );
  const points = plan
    ? await db.query(
        sql`SELECT a.* FROM auricular_point a JOIN treatment_point p ON p.point_id=a.id WHERE p.plan_version_id=${plan.id}`,
      )
    : [];
  const sessions = await db.query(
    sql`SELECT id,occurred_at FROM treatment_session WHERE treatment_id=${id} ORDER BY occurred_at,id`,
  );
  return {
    treatment,
    plan,
    symptoms,
    occurrences,
    appointments,
    points,
    sessions,
  };
}
async function edit<T>(
  db: Database,
  actor: Actor,
  id: string,
  run: (tx: Database, t: Treatment) => Promise<T>,
) {
  professional(actor);
  return db.transaction(async (tx) => {
    const t = await accessTreatment(tx, actor, id);
    await lockPatient(tx, t.patient_id);
    const current = await accessTreatment(tx, actor, id, true);
    return run(tx, current);
  });
}
export function registerTreatments(app: FastifyInstance, db: Database) {
  app.get("/api/v1/treatments", async (req) => {
    const actor = actorOf(req);
    const filter = pagination
      .extend({
        patientId: uuid.optional(),
        status: z.enum(["draft", "active", "finished"]).optional(),
      })
      .parse(req.query);
    const scope =
      actor.role === "patient"
        ? sql`t.patient_id=${actor.id} AND t.status<>'draft'`
        : sql`t.professional_id=${actor.id} AND EXISTS(SELECT 1 FROM patient_professional_link l WHERE l.patient_id=t.patient_id AND l.professional_id=${actor.id} AND l.status='active')`;
    await audit(db, actor, "treatments.list");
    return {
      items: await db.query(
        sql`SELECT t.*,u.display_name AS patient_name FROM treatment t JOIN app_user u ON u.id=t.patient_id WHERE ${scope} AND (${filter.patientId ?? null}::uuid IS NULL OR t.patient_id=${filter.patientId ?? null}) AND (${filter.status ?? null}::text IS NULL OR t.status=${filter.status ?? null}) AND t.title ILIKE ${"%" + filter.q + "%"} ORDER BY t.updated_at DESC,t.id LIMIT ${filter.limit} OFFSET ${(filter.page - 1) * filter.limit}`,
      ),
      page: filter.page,
      limit: filter.limit,
    };
  });
  app.post("/api/v1/treatments", async (req, reply) => {
    const actor = actorOf(req);
    professional(actor);
    const { patientId, plan } = z
        .object({ patientId: uuid, plan: planInput })
        .strict()
        .parse(req.body),
      id = randomUUID();
    await db.transaction(async (tx) => {
      await lockPatient(tx, patientId);
      await activeLink(tx, patientId, actor.id);
      await tx.query(
        sql`INSERT INTO treatment(id,patient_id,professional_id,title,starts_on,ends_on,timezone) VALUES(${id},${patientId},${actor.id},${plan.title},${plan.startsOn},${plan.endsOn},${plan.timezone})`,
      );
      await tx.query(
        sql`INSERT INTO treatment_plan(id,draft) VALUES(${id},${JSON.stringify(plan)}::jsonb)`,
      );
      await audit(tx, actor, "treatment.create", id);
    });
    return reply.code(201).send({ id, version: 1 });
  });
  app.get("/api/v1/treatments/:id", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    await accessTreatment(db, actor, id);
    await audit(db, actor, "treatment.read", id);
    const data = await snapshot(db, id);
    const versions = await db.query(
      sql`SELECT version,published_at,content FROM treatment_plan_version WHERE treatment_id=${id} ORDER BY version DESC`,
    );
    const draft =
      actor.role === "professional"
        ? (
            await db.query(sql`SELECT draft FROM treatment_plan WHERE id=${id}`)
          )[0]?.draft
        : undefined;
    return { ...data, versions, draft };
  });
  app.patch("/api/v1/treatments/:id", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    const { version, plan } = z
      .object({ version: z.number().int().positive(), plan: planInput })
      .strict()
      .parse(req.body);
    return edit(db, actor, id, async (tx, t) => {
      if (t.version !== version) throw new ApiError(409, "VERSION_CONFLICT");
      if (t.status === "finished")
        throw new ApiError(409, "TREATMENT_FINISHED");
      // Draft editing does not change the published treatment until publication.
      await tx.query(
        sql`UPDATE treatment_plan SET draft=${JSON.stringify(plan)}::jsonb WHERE id=${id}`,
      );
      await tx.query(
        sql`UPDATE treatment SET version=version+1,updated_at=now() WHERE id=${id}`,
      );
      await audit(tx, actor, "plan.draft.update", id);
      return { id, version: version + 1 };
    });
  });
  app.post("/api/v1/treatments/:id/publish", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params),
      { version } = z
        .object({ version: z.number().int().positive() })
        .strict()
        .parse(req.body);
    return edit(db, actor, id, async (tx, t) => {
      if (t.version !== version) throw new ApiError(409, "VERSION_CONFLICT");
      if (t.status === "finished")
        throw new ApiError(409, "TREATMENT_FINISHED");
      const [stored] = await tx.query<{ draft: Plan; current_version: number }>(
        sql`SELECT draft,current_version FROM treatment_plan WHERE id=${id}`,
      );
      const plan = planInput.parse(stored!.draft),
        nextVersion = stored!.current_version + 1,
        planId = randomUUID();
      if (
        t.status === "active" &&
        (plan.startsOn !== t.starts_on || plan.timezone !== t.timezone)
      )
        throw new ApiError(422, "START_AND_TIMEZONE_IMMUTABLE");
      const existing = await tx.query<{
        id: string;
        name: string;
        min_value: number;
        max_value: number;
      }>(sql`SELECT * FROM treatment_symptom WHERE treatment_id=${id}`);
      for (const symptom of plan.symptoms) {
        const old = existing.find((s) => s.id === symptom.id);
        if (
          old &&
          (old.name !== symptom.name ||
            old.min_value !== symptom.min ||
            old.max_value !== symptom.max)
        )
          throw new ApiError(422, "SYMPTOM_DEFINITION_IMMUTABLE");
        if (!old)
          await tx.query(
            sql`INSERT INTO treatment_symptom(id,treatment_id,name,min_value,max_value) VALUES(${symptom.id},${id},${symptom.name},${symptom.min},${symptom.max})`,
          );
      }
      for (const pointId of plan.pointIds) {
        if (
          !(
            await tx.query(
              sql`SELECT id FROM auricular_point WHERE id=${pointId} AND professional_id=${actor.id}`,
            )
          ).length
        )
          throw new ApiError(422, "POINT_NOT_REVIEWED");
      }
      await tx.query(
        sql`INSERT INTO treatment_plan_version(id,treatment_id,version,content) VALUES(${planId},${id},${nextVersion},${JSON.stringify(plan)}::jsonb)`,
      );
      for (const text of plan.instructions)
        await tx.query(
          sql`INSERT INTO treatment_instruction(id,plan_version_id,text) VALUES(${randomUUID()},${planId},${text})`,
        );
      for (const pointId of plan.pointIds)
        await tx.query(
          sql`INSERT INTO treatment_point(plan_version_id,point_id) VALUES(${planId},${pointId})`,
        );
      const now = new Date();
      // Elapsed occurrences and confirmations are immutable. Replace future unrecorded slots only.
      await tx.query(
        sql`UPDATE scheduled_stimulation SET status='cancelled' WHERE treatment_id=${id} AND scheduled_at>${now.toISOString()} AND NOT EXISTS(SELECT 1 FROM stimulation_log l WHERE l.scheduled_id=scheduled_stimulation.id)`,
      );
      for (const at of schedule(plan, t.status === "active" ? now : undefined))
        await tx.query(
          sql`INSERT INTO scheduled_stimulation(id,treatment_id,plan_version_id,scheduled_at) VALUES(${randomUUID()},${id},${planId},${at}) ON CONFLICT(treatment_id,scheduled_at) DO UPDATE SET status='scheduled',plan_version_id=EXCLUDED.plan_version_id WHERE scheduled_stimulation.scheduled_at>${now.toISOString()}`,
        );
      await tx.query(
        sql`UPDATE follow_up_appointment SET status='cancelled' WHERE treatment_id=${id} AND status='scheduled'`,
      );
      if (plan.followUpAt)
        await tx.query(
          sql`INSERT INTO follow_up_appointment(id,treatment_id,scheduled_at) VALUES(${randomUUID()},${id},${plan.followUpAt})`,
        );
      await tx.query(
        sql`UPDATE treatment_plan SET current_version=${nextVersion} WHERE id=${id}`,
      );
      await tx.query(
        sql`UPDATE treatment SET title=${plan.title},starts_on=${plan.startsOn},ends_on=${plan.endsOn},timezone=${plan.timezone},status='active',version=version+1,updated_at=now() WHERE id=${id}`,
      );
      await change(tx, t.patient_id, "treatment", id, await snapshot(tx, id));
      await audit(tx, actor, "plan.publish", id);
      return { id, version: version + 1, planVersion: nextVersion };
    });
  });
  app.post("/api/v1/treatments/:id/finish", async (req) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    return edit(db, actor, id, async (tx, t) => {
      if (t.status === "draft") throw new ApiError(409, "PLAN_NOT_PUBLISHED");
      if (t.status !== "finished") {
        await tx.query(
          sql`UPDATE treatment SET status='finished',finished_at=now(),version=version+1,updated_at=now() WHERE id=${id}`,
        );
        await tx.query(
          sql`UPDATE scheduled_stimulation SET status='cancelled' WHERE treatment_id=${id} AND scheduled_at>now()`,
        );
        await tx.query(
          sql`UPDATE follow_up_appointment SET status='cancelled' WHERE treatment_id=${id} AND status='scheduled' AND scheduled_at>now()`,
        );
        await change(tx, t.patient_id, "treatment", id, await snapshot(tx, id));
        await audit(tx, actor, "treatment.finish", id);
      }
      return { ok: true };
    });
  });
  app.get("/api/v1/treatments/:id/sessions", async (req) => {
    const actor = actorOf(req);
    professional(actor);
    const { id } = idParams.parse(req.params);
    await accessTreatment(db, actor, id);
    await audit(db, actor, "sessions.read", id);
    const { page, limit } = pagination.parse(req.query);
    return {
      items: await db.query(
        sql`SELECT * FROM treatment_session WHERE treatment_id=${id} ORDER BY occurred_at DESC LIMIT ${limit} OFFSET ${(page - 1) * limit}`,
      ),
    };
  });
  app.post("/api/v1/treatments/:id/sessions", async (req, reply) => {
    const actor = actorOf(req),
      { id } = idParams.parse(req.params);
    const body = z
      .object({ occurredAt: instant, notes: z.string().max(5000).optional() })
      .strict()
      .parse(req.body);
    const sessionId = randomUUID();
    await edit(db, actor, id, async (tx, treatment) => {
      await tx.query(
        sql`INSERT INTO treatment_session(id,treatment_id,occurred_at,notes) VALUES(${sessionId},${id},${body.occurredAt},${body.notes ?? null})`,
      );
      await audit(tx, actor, "session.create", id);
      if (treatment.status !== "draft")
        await change(
          tx,
          treatment.patient_id,
          "treatment",
          id,
          await snapshot(tx, id),
        );
    });
    return reply.code(201).send({ id: sessionId });
  });
  app.get("/api/v1/points", async (req) => {
    const actor = actorOf(req);
    professional(actor);
    return {
      items: await db.query(
        sql`SELECT * FROM auricular_point WHERE professional_id=${actor.id} ORDER BY label LIMIT 100`,
      ),
    };
  });
  app.post("/api/v1/points", async (req, reply) => {
    const actor = actorOf(req);
    professional(actor);
    const p = z
        .object({
          label: z.string().trim().min(1).max(100),
          source: z.string().trim().min(1).max(500),
          x: z.number().min(0).max(1),
          y: z.number().min(0).max(1),
          reviewed: z.literal(true),
        })
        .strict()
        .parse(req.body),
      id = randomUUID();
    await db.transaction(async (tx) => {
      await tx.query(
        sql`INSERT INTO auricular_point(id,professional_id,label,source,x,y,reviewed_at) VALUES(${id},${actor.id},${p.label},${p.source},${p.x},${p.y},now())`,
      );
      await audit(tx, actor, "point.review", id);
    });
    return reply.code(201).send({ id });
  });
}
