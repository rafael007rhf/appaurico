import { drizzle } from "drizzle-orm/node-postgres";
import { sql, type SQL } from "drizzle-orm";
import pg from "pg";
import { normalizeDatabaseValue } from "./serialization.js";
export { sql };
export type Row = Record<string, unknown>;
export interface Database {
  query<T = Row>(statement: SQL): Promise<T[]>;
  transaction<T>(run: (tx: Database) => Promise<T>): Promise<T>;
  close(): Promise<void>;
}
export function connect(connectionString: string): Database {
  const pool = new pg.Pool({ connectionString, max: 10 });
  const db = drizzle(pool);
  const wrap = (
    executor: Pick<typeof db, "execute" | "transaction">,
  ): Database => ({
    async query<T>(q: SQL) {
      const result = await executor.execute(q);
      return normalizeDatabaseValue(result.rows) as T[];
    },
    transaction: (run) => executor.transaction((tx) => run(wrap(tx))),
    close: () => pool.end(),
  });
  return wrap(db);
}
