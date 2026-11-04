import pg from "pg";
const timestamps = new Set([
  "created_at",
  "updated_at",
  "scheduled_at",
  "occurred_at",
  "published_at",
  "finished_at",
  "expires_at",
  "last_seen_at",
]);
/** Normalizes both driver rows and older JSONB change payloads at the API boundary. */
export function normalizeDatabaseValue(value: unknown, key?: string): unknown {
  if (value === null || value === undefined) return value;
  if (value instanceof Date) return value.toISOString();
  if (typeof value === "string" && key && timestamps.has(key)) {
    const parsed = value.includes("T")
      ? new Date(value)
      : (pg.types.getTypeParser(1184, "text")(value) as Date | null);
    if (parsed instanceof Date && !Number.isNaN(parsed.getTime()))
      return parsed.toISOString();
    throw new Error("Invalid database timestamp");
  }
  if (Array.isArray(value)) return value.map((v) => normalizeDatabaseValue(v));
  if (typeof value === "object")
    return Object.fromEntries(
      Object.entries(value).map(([k, v]) => [k, normalizeDatabaseValue(v, k)]),
    );
  return value;
}
