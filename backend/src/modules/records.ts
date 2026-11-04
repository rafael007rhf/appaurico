import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { sql, type Database, type Row } from "../db.js";
import { actorOf, hash } from "./auth.js";
import {
  ApiError,
  accessTreatment,
  audit,
  change,
  idParams,
  instant,
  lockPatient,
  pagination,
  patient,
  uuid,
  type Actor,
} from "./common.js";
import { assertRecordedTime } from "./plans.js";
const symptom = z
  .object({
    id: uuid,
    treatmentId: uuid,
    symptomId: uuid,
    level: z.number().int().min(0).max(100),
    note: z.string().max(2000).nullable().default(null),
    occurredAt: instant,
    correctionOf: uuid.nullable().default(null),
    sessionId: uuid.nullable().default(null),
  })
  .strict();
const stimulation = z
  .object({
    id: uuid,
    treatmentId: uuid,
    scheduledId: uuid,
    occurredAt: instant,
  })
  .strict();
export const operation = z.discriminatedUnion("kind", [
  z
    .object({ operationId: uuid, kind: z.literal("symptom"), data: symptom })
    .strict(),
  z
    .object({
      operationId: uuid,
      kind: z.literal("stimulation"),
      data: stimulation,
    })
    .strict(),
]);
type Operation = z.infer<typeof operation>;
export async function applyOperation(
  db: Database,
  actor: Actor,
  deviceId: string,
  op: Operation,
) {
  patient(actor);
  return db.transaction(async (tx) => {
    await lockPatient(tx, actor.id);
    const t = await accessTreatment(tx, actor, op.data.treatmentId, true);
    const digest = hash(JSON.stringify(op));
    const [receipt] = await tx.query<{ payload_hash: string; result: Row }>(
      sql`SELECT payload_hash,result FROM sync_operation WHERE user_id=${actor.id} AND operation_id=${op.operationId}`,
    );
    if (receipt) {
      if (receipt.payload_hash !== digest)
        throw new ApiError(409, "IDEMPOTENCY_MISMATCH");
      return receipt.result;
    }
    if (t.status !== "active") throw new ApiError(409, "TREATMENT_NOT_ACTIVE");
    assertRecordedTime(op.data.occurredAt, t.starts_on, t.ends_on, t.timezone);
    await tx.query(
      sql`INSERT INTO device_registration(id,user_id) VALUES(${deviceId},${actor.id}) ON CONFLICT(user_id,id) DO UPDATE SET last_seen_at=now()`,
    );
    let record: Row;
    if (op.kind === "symptom") {
      const data = op.data;
      const [s] = await tx.query<{ min_value: number; max_value: number }>(
        sql`SELECT min_value,max_value FROM treatment_symptom WHERE id=${data.symptomId} AND treatment_id=${t.id}`,
      );
      if (!s || data.level < s.min_value || data.level > s.max_value)
        throw new ApiError(422, "INVALID_SYMPTOM_SCALE");
      if (data.correctionOf) {
        if (
          !(
            await tx.query(
              sql`SELECT id FROM symptom_log WHERE id=${data.correctionOf} AND patient_id=${actor.id} AND treatment_id=${t.id} AND symptom_id=${data.symptomId}`,
            )
          ).length
        )
          throw new ApiError(422, "INVALID_CORRECTION");
        if (
          (
            await tx.query(
              sql`SELECT id FROM symptom_log WHERE correction_of=${data.correctionOf}`,
            )
          ).length
        )
          throw new ApiError(409, "ALREADY_CORRECTED");
      }
      if (
        data.sessionId &&
        !(
          await tx.query(
            sql`SELECT id FROM treatment_session WHERE id=${data.sessionId} AND treatment_id=${t.id}`,
          )
        ).length
      )
        throw new ApiError(422, "INVALID_SESSION");
      const [inserted] = await tx.query(
        sql`INSERT INTO symptom_log(id,treatment_id,patient_id,symptom_id,level,note,occurred_at,correction_of,session_id) VALUES(${data.id},${t.id},${actor.id},${data.symptomId},${data.level},${data.note},${data.occurredAt},${data.correctionOf},${data.sessionId}) RETURNING *`,
      );
      record = inserted!;
    } else {
      const data = op.data;
      const [slot] = await tx.query<{ scheduled_at: string; status: string }>(
        sql`SELECT scheduled_at,status FROM scheduled_stimulation WHERE id=${data.scheduledId} AND treatment_id=${t.id}`,
      );
      if (
        !slot ||
        slot.status !== "scheduled" ||
        new Date(slot.scheduled_at) > new Date(data.occurredAt)
      )
        throw new ApiError(422, "INVALID_OCCURRENCE");
      const [existing] = await tx.query(
        sql`SELECT * FROM stimulation_log WHERE scheduled_id=${data.scheduledId}`,
      );
      // Multiple devices can confirm the same occurrence; acknowledge its canonical record.
      if (existing) record = existing;
      else {
        const [inserted] = await tx.query(
          sql`INSERT INTO stimulation_log(id,treatment_id,patient_id,scheduled_id,occurred_at) VALUES(${data.id},${t.id},${actor.id},${data.scheduledId},${data.occurredAt}) RETURNING *`,
        );
        record = inserted!;
      }
    }
    const result = {
      operationId: op.operationId,
      status: "acknowledged",
      record,
    };
    await tx.query(
      sql`INSERT INTO sync_operation(user_id,operation_id,device_id,payload_hash,result) VALUES(${actor.id},${op.operationId},${deviceId},${digest},${JSON.stringify(result)}::jsonb)`,
    );
    await change(tx, actor.id, op.kind, String(record.id), record);
    await audit(tx, actor, `${op.kind}.record`, t.id);
    return result;
  });
}
export function registerRecords(app: FastifyInstance, db: Database) {
  for (const [path, kind, table] of [
    ["symptom-logs", "symptom", "symptom_log"],
    ["stimulations", "stimulation", "stimulation_log"],
  ] as const) {
    app.get(`/api/v1/treatments/:id/${path}`, async (req) => {
      const actor = actorOf(req),
        { id } = idParams.parse(req.params);
      await accessTreatment(db, actor, id);
      await audit(db, actor, `${kind}.read`, id);
      const { page, limit } = pagination.parse(req.query);
      const items = await db.query(
        sql`SELECT * FROM ${sql.identifier(table)} WHERE treatment_id=${id} ORDER BY occurred_at DESC,id LIMIT ${limit} OFFSET ${(page - 1) * limit}`,
      );
      return { items, page, limit };
    });
    app.post(`/api/v1/treatments/:id/${path}`, async (req, reply) => {
      const { id } = idParams.parse(req.params);
      const envelope = z
        .object({ deviceId: uuid, operation: operation })
        .strict()
        .parse(req.body);
      if (
        envelope.operation.kind !== kind ||
        envelope.operation.data.treatmentId !== id
      )
        throw new ApiError(422, "RESOURCE_MISMATCH");
      return reply
        .code(201)
        .send(
          await applyOperation(
            db,
            actorOf(req),
            envelope.deviceId,
            envelope.operation,
          ),
        );
    });
  }
  app.post("/api/v1/sync/push", async (req) => {
    const actor = actorOf(req);
    patient(actor);
    const body = z
      .object({ deviceId: uuid, operations: z.array(operation).min(1).max(50) })
      .strict()
      .parse(req.body);
    const results = [];
    for (const op of body.operations) {
      try {
        results.push(await applyOperation(db, actor, body.deviceId, op));
      } catch (error) {
        if (error instanceof ApiError)
          results.push({
            operationId: op.operationId,
            status: "rejected",
            code: error.code,
          });
        else throw error;
      }
    }
    return { results };
  });
  app.get("/api/v1/sync/pull", async (req) => {
    const actor = actorOf(req);
    patient(actor);
    const { cursor, limit } = z
      .object({
        cursor: z
          .string()
          .regex(/^\d{1,18}$/)
          .default("0"),
        limit: z.coerce.number().int().min(1).max(100).default(50),
      })
      .parse(req.query);
    const items = await db.query<{ revision: string }>(
      sql`SELECT revision::text,kind,resource_id,payload FROM sync_change WHERE patient_id=${actor.id} AND revision>${cursor}::bigint ORDER BY revision LIMIT ${limit + 1}`,
    );
    const changes = items.slice(0, limit);
    await audit(db, actor, "sync.pull");
    return {
      changes,
      cursor: changes.at(-1)?.revision ?? cursor,
      hasMore: items.length > limit,
    };
  });
  app.get("/api/v1/export", async (req) => {
    const actor = actorOf(req);
    patient(actor);
    await audit(db, actor, "patient.export");
    return {
      generatedAt: new Date().toISOString(),
      profile: actor,
      treatments: await db.query(
        sql`SELECT * FROM treatment WHERE patient_id=${actor.id} AND status<>'draft'`,
      ),
      symptomLogs: await db.query(
        sql`SELECT * FROM symptom_log WHERE patient_id=${actor.id}`,
      ),
      stimulations: await db.query(
        sql`SELECT * FROM stimulation_log WHERE patient_id=${actor.id}`,
      ),
      consents: await db.query(
        sql`SELECT * FROM consent_record WHERE patient_id=${actor.id}`,
      ),
    };
  });
}
