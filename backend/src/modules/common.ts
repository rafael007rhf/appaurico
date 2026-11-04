import { randomUUID } from "node:crypto";
import { z } from "zod";
import { sql, type Database } from "../db.js";
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
  ) {
    super(code);
  }
}
export type Actor = {
  operations_access?: boolean;
  id: string;
  role: "patient" | "professional";
  email: string | null;
  email_verified: boolean;
  display_name: string;
};
export const uuid = z.string().uuid();
export const idParams = z.object({ id: uuid });
export const pagination = z.object({
  page: z.coerce.number().int().min(1).max(10000).default(1),
  limit: z.coerce.number().int().min(1).max(100).default(30),
  q: z.string().max(100).default(""),
});
export const instant = z.iso.datetime({ offset: true });
export function professional(actor: Actor) {
  if (actor.role !== "professional")
    throw new ApiError(403, "PROFESSIONAL_REQUIRED");
}
export function patient(actor: Actor) {
  if (actor.role !== "patient") throw new ApiError(403, "PATIENT_REQUIRED");
}
export async function audit(
  db: Database,
  actor: Actor,
  action: string,
  resourceId: string | null = null,
) {
  await db.query(
    sql`INSERT INTO audit_event(id,actor_id,action,resource_id) VALUES(${randomUUID()},${actor.id},${action},${resourceId})`,
  );
}
// Every patient write locks this row BEFORE changing resources or checking links.
// Revisions therefore follow commit order, including concurrent devices and revocations.
export async function lockPatient(db: Database, id: string) {
  await db.query(sql`SELECT id FROM app_user WHERE id=${id} FOR UPDATE`);
}
export async function change(
  db: Database,
  patientId: string,
  kind: string,
  id: string,
  payload: unknown,
) {
  const [row] = await db.query<{ revision: string }>(
    sql`UPDATE app_user SET revision=revision+1 WHERE id=${patientId} RETURNING revision`,
  );
  await db.query(
    sql`INSERT INTO sync_change(patient_id,revision,kind,resource_id,payload) VALUES(${patientId},${row!.revision},${kind},${id},${JSON.stringify(payload)}::jsonb)`,
  );
}
export async function activeLink(
  db: Database,
  patientId: string,
  professionalId: string,
) {
  const rows = await db.query(
    sql`SELECT id FROM patient_professional_link WHERE patient_id=${patientId} AND professional_id=${professionalId} AND status='active'`,
  );
  if (!rows.length) throw new ApiError(404, "RESOURCE_NOT_FOUND");
}
export type Treatment = {
  id: string;
  patient_id: string;
  professional_id: string;
  title: string;
  status: string;
  version: number;
  starts_on: string;
  ends_on: string;
  timezone: string;
  finished_at: string | null;
};
export async function accessTreatment(
  db: Database,
  actor: Actor,
  id: string,
  write = false,
): Promise<Treatment> {
  const [t] = await db.query<Treatment>(
    sql`SELECT * FROM treatment WHERE id=${id}`,
  );
  if (
    !t ||
    (actor.role === "patient"
      ? t.patient_id !== actor.id
      : t.professional_id !== actor.id)
  )
    throw new ApiError(404, "RESOURCE_NOT_FOUND");
  if (actor.role === "professional" || write)
    await activeLink(db, t.patient_id, t.professional_id);
  if (actor.role === "patient" && t.status === "draft")
    throw new ApiError(404, "RESOURCE_NOT_FOUND");
  return t;
}
