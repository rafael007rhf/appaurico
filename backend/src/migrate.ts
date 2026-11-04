import { readFile } from "node:fs/promises";
import { connect, sql } from "./db.js";
const db = connect(process.env.DATABASE_URL ?? "");
try {
  await db.transaction(async (tx) => {
    await tx.query(sql`SELECT pg_advisory_xact_lock(30001)`);
    await tx.query(
      sql`CREATE TABLE IF NOT EXISTS schema_migration (version text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())`,
    );
    if (
      (
        await tx.query(
          sql`SELECT version FROM schema_migration WHERE version='0001'`,
        )
      ).length
    )
      return;
    const source = await readFile(
      new URL("../migrations/0001_platform.sql", import.meta.url),
      "utf8",
    );
    for (const statement of source.split(";").filter((x) => x.trim()))
      await tx.query(sql.raw(statement));
    await tx.query(sql`INSERT INTO schema_migration(version) VALUES('0001')`);
  });
  console.log("Database migrations applied.");
} finally {
  await db.close();
}
