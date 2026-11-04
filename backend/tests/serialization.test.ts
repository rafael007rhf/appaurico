import { test } from "node:test";
import assert from "node:assert/strict";
import { normalizeDatabaseValue } from "../src/serialization.js";
test("PostgreSQL rows and pre-existing JSONB sync timestamps satisfy the native ISO contract", () => {
  const source = {
    starts_on: "2026-11-04",
    occurred_at: "2026-11-03 21:00:00-03",
    payload: { occurrences: [{ scheduled_at: "2026-11-04 00:00:00+00" }] },
    note: "2026-11-03 21:00:00-03",
    finished_at: null,
  };
  assert.deepEqual(
    normalizeDatabaseValue(normalizeDatabaseValue(source)),
    normalizeDatabaseValue(source),
  );
  assert.deepEqual(normalizeDatabaseValue(source), {
    starts_on: "2026-11-04",
    occurred_at: "2026-11-04T00:00:00.000Z",
    payload: { occurrences: [{ scheduled_at: "2026-11-04T00:00:00.000Z" }] },
    note: source.note,
    finished_at: null,
  });
});
