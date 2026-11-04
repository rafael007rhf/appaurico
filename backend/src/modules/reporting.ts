import type { FastifyInstance } from "fastify";
import PDFDocument from "pdfkit";
import { z } from "zod";
import { sql, type Database } from "../db.js";
import { actorOf } from "./auth.js";
import { accessTreatment, audit, professional, uuid } from "./common.js";
import { adherence } from "./plans.js";
export function registerReporting(app: FastifyInstance, db: Database) {
  app.get("/api/v1/dashboard", async (req) => {
    const actor = actorOf(req);
    professional(actor);
    const scope = sql`t.professional_id=${actor.id} AND EXISTS(SELECT 1 FROM patient_professional_link l WHERE l.patient_id=t.patient_id AND l.professional_id=${actor.id} AND l.status='active')`;
    const [counts] = await db.query(
      sql`SELECT count(*) FILTER(WHERE t.status='active')::int AS active_treatments,count(*) FILTER(WHERE t.status='draft')::int AS drafts FROM treatment t WHERE ${scope}`,
    );
    const [patients] = await db.query(
      sql`SELECT count(*)::int AS active_patients FROM patient_professional_link WHERE professional_id=${actor.id} AND status='active'`,
    );
    const returns = await db.query(
      sql`SELECT a.*,t.title,u.display_name FROM follow_up_appointment a JOIN treatment t ON t.id=a.treatment_id JOIN app_user u ON u.id=t.patient_id WHERE ${scope} AND a.status='scheduled' AND a.scheduled_at>=now() ORDER BY a.scheduled_at LIMIT 10`,
    );
    const recent = await db.query(
      sql`SELECT l.id,l.occurred_at,l.level,s.name AS symptom,u.display_name,t.id AS treatment_id FROM symptom_log l JOIN treatment t ON t.id=l.treatment_id JOIN treatment_symptom s ON s.id=l.symptom_id JOIN app_user u ON u.id=l.patient_id WHERE ${scope} ORDER BY l.created_at DESC LIMIT 10`,
    );
    const missing = await db.query(
      sql`SELECT DISTINCT u.id,u.display_name FROM treatment t JOIN app_user u ON u.id=t.patient_id WHERE ${scope} AND t.status='active' AND t.starts_on<=CURRENT_DATE AND t.ends_on>=CURRENT_DATE AND NOT EXISTS(SELECT 1 FROM symptom_log l WHERE l.treatment_id=t.id AND l.occurred_at>now()-interval '3 days') LIMIT 30`,
    );
    await audit(db, actor, "dashboard.read");
    return { ...counts, ...patients, returns, recent, missing };
  });
  app.get("/api/v1/reports/:treatmentId", async (req, reply) => {
    const actor = actorOf(req),
      { treatmentId } = z.object({ treatmentId: uuid }).parse(req.params);
    const query = z
      .object({
        format: z.enum(["json", "pdf"]).default("json"),
        from: z.iso.datetime({ offset: true }).optional(),
        to: z.iso.datetime({ offset: true }).optional(),
      })
      .parse(req.query);
    const treatment = await accessTreatment(db, actor, treatmentId);
    await audit(db, actor, "report.read", treatmentId);
    const from = query.from ?? `${treatment.starts_on}T00:00:00Z`,
      to = query.to ?? new Date().toISOString();
    const symptomLogs = await db.query<{
      id: string;
      symptom_id: string;
      level: number;
      note: string | null;
      occurred_at: string;
      name: string;
      correction_of: string | null;
    }>(
      sql`SELECT l.*,s.name FROM symptom_log l JOIN treatment_symptom s ON s.id=l.symptom_id WHERE l.treatment_id=${treatmentId} AND l.occurred_at BETWEEN ${from} AND ${to} ORDER BY l.occurred_at,l.id`,
    );
    const occurrences = await db.query<{
      id: string;
      scheduled_at: string;
      status: string;
    }>(
      sql`SELECT id,scheduled_at,status FROM scheduled_stimulation WHERE treatment_id=${treatmentId} AND scheduled_at BETWEEN ${from} AND ${to}`,
    );
    const stimulations = await db.query<{ scheduled_id: string }>(
      sql`SELECT * FROM stimulation_log WHERE treatment_id=${treatmentId}`,
    );
    const [patient] = await db.query<{ display_name: string }>(
      sql`SELECT display_name FROM app_user WHERE id=${treatment.patient_id}`,
    );
    const data = {
      generatedAt: new Date().toISOString(),
      patient: patient!.display_name,
      treatment,
      from,
      to,
      symptomLogs,
      adherence: adherence(occurrences, stimulations, new Date()),
      notice:
        "Registros informados pelo paciente. Os dados não estabelecem diagnóstico ou eficácia clínica.",
    };
    if (query.format === "json") return data;
    const doc = new PDFDocument({ margin: 48, bufferPages: true });
    reply
      .type("application/pdf")
      .header(
        "Content-Disposition",
        'attachment; filename="aurico-acompanhamento.pdf"',
      );
    doc.fontSize(22).text("AURICO | Acompanhamento");
    doc
      .moveDown()
      .fontSize(12)
      .text(data.patient)
      .text(treatment.title)
      .text(`Período: ${from.slice(0, 10)} a ${to.slice(0, 10)}`)
      .text(`Gerado: ${data.generatedAt}`);
    doc
      .moveDown()
      .text(
        `Estimulações: ${data.adherence.recorded} / ${data.adherence.expected} (${data.adherence.percentage ?? "—"}%)`,
      );
    doc.moveDown().fontSize(10).text(data.notice);
    const corrected = new Set(
      symptomLogs.map((l) => l.correction_of).filter(Boolean),
    );
    const groups = Map.groupBy(
      symptomLogs.filter((l) => !corrected.has(l.id)),
      (l) => l.symptom_id,
    );
    for (const logs of groups.values()) {
      doc.moveDown().fontSize(12).text(logs[0]!.name);
      if (logs.length < 2) {
        doc.fontSize(10).text("Dados insuficientes para evolução.");
        continue;
      }
      if (doc.y > 600) doc.addPage();
      const top = doc.y + 8,
        left = 50,
        width = 480,
        height = 90;
      const minTime = new Date(logs[0]!.occurred_at).getTime(),
        maxTime = new Date(logs.at(-1)!.occurred_at).getTime();
      const max = Math.max(1, ...logs.map((l) => l.level));
      doc.strokeColor("#7CA289");
      logs.forEach((log, i) => {
        const x =
            left +
            ((new Date(log.occurred_at).getTime() - minTime) /
              Math.max(1, maxTime - minTime)) *
              width,
          y = top + height - (log.level / max) * height;
        if (i === 0) doc.moveTo(x, y);
        else doc.lineTo(x, y);
      });
      doc.stroke();
      doc.y = top + height + 15;
    }
    doc.addPage().fontSize(14).text("Histórico de registros e correções");
    for (const log of symptomLogs)
      doc
        .moveDown(0.5)
        .fontSize(10)
        .text(
          `${new Date(log.occurred_at).toISOString()} | ${log.name}: ${log.level}${log.correction_of ? " (correção)" : ""}${corrected.has(log.id) ? " (substituído por correção)" : ""}`,
        )
        .text(log.note ?? "");
    doc.end();
    return reply.send(doc);
  });
}
