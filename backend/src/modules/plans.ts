import { DateTime } from "luxon";
import { z } from "zod";
import { ApiError, uuid } from "./common.js";
const symptom = z
  .object({
    id: uuid,
    name: z.string().trim().min(1).max(120),
    min: z.number().int().min(0).max(99),
    max: z.number().int().min(1).max(100),
  })
  .strict()
  .refine((s) => s.max > s.min);
export const planInput = z
  .object({
    title: z.string().trim().min(1).max(160),
    startsOn: z.iso.date(),
    endsOn: z.iso.date(),
    timezone: z
      .string()
      .max(80)
      .refine((zone) => DateTime.now().setZone(zone).isValid),
    symptoms: z.array(symptom).min(1).max(20),
    times: z
      .array(z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/))
      .min(1)
      .max(8),
    instructions: z.array(z.string().trim().min(1).max(2000)).min(1).max(20),
    pointIds: z.array(uuid).max(30).default([]),
    followUpAt: z.iso.datetime({ offset: true }).nullable().default(null),
  })
  .strict()
  .superRefine((p, ctx) => {
    const days = DateTime.fromISO(p.endsOn).diff(
      DateTime.fromISO(p.startsOn),
      "days",
    ).days;
    if (days < 0 || days > 365)
      ctx.addIssue({
        code: "custom",
        message: "O plano deve ter entre 1 e 366 dias.",
      });
    if (
      new Set(p.times).size !== p.times.length ||
      new Set(p.symptoms.map((s) => s.id)).size !== p.symptoms.length ||
      new Set(p.pointIds).size !== p.pointIds.length
    )
      ctx.addIssue({ code: "custom", message: "Itens duplicados." });
  });
export type Plan = z.infer<typeof planInput>;
export function schedule(plan: Plan, after?: Date): string[] {
  let day = DateTime.fromISO(plan.startsOn, { zone: plan.timezone });
  const end = DateTime.fromISO(plan.endsOn, { zone: plan.timezone });
  const result: string[] = [];
  while (day <= end) {
    for (const time of [...plan.times].sort()) {
      const local = DateTime.fromISO(`${day.toISODate()}T${time}`, {
        zone: plan.timezone,
      });
      // A nonexistent DST wall-clock time is skipped; overlap uses the earlier instant once.
      if (local.toFormat("HH:mm") !== time) continue;
      const instant =
        local
          .getPossibleOffsets()
          .sort((a, b) => a.toMillis() - b.toMillis())[0] ?? local;
      if (!after || instant.toMillis() > after.getTime())
        result.push(instant.toUTC().toISO()!);
    }
    day = day.plus({ days: 1 });
  }
  return result;
}
export function assertRecordedTime(
  value: string,
  startsOn: string,
  endsOn: string,
  zone: string,
  now = new Date(),
) {
  const at = DateTime.fromISO(value).setZone(zone);
  if (
    at.toMillis() > now.getTime() + 60000 ||
    at.toISODate()! < startsOn ||
    at.toISODate()! > endsOn
  )
    throw new ApiError(422, "OUTSIDE_TREATMENT_PERIOD");
}
export function adherence(
  expected: { id: string; scheduled_at: string; status: string }[],
  recorded: { scheduled_id: string }[],
  now: Date,
) {
  const due = new Set(
    expected
      .filter(
        (o) => o.status === "scheduled" && new Date(o.scheduled_at) <= now,
      )
      .map((o) => o.id),
  );
  const done = new Set(
    recorded.filter((r) => due.has(r.scheduled_id)).map((r) => r.scheduled_id),
  );
  return {
    expected: due.size,
    recorded: done.size,
    percentage: due.size ? Math.round((done.size * 100) / due.size) : null,
  };
}
