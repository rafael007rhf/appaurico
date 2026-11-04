import { after, before, test } from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { generateKeyPair, jwtVerify, SignJWT } from "jose";
import { buildApp } from "../src/app.js";
import { sql, type Database } from "../src/db.js";
import { schedule, adherence, planInput } from "../src/modules/plans.js";
const pg = new PGlite(),
  orm = drizzle(pg);
const wrap = (db: Pick<typeof orm, "execute" | "transaction">): Database => ({
  query: async <T>(q) => (await db.execute(q)).rows as T[],
  transaction: (run) => db.transaction((tx) => run(wrap(tx))),
  close: () => pg.close(),
});
const db = wrap(orm),
  keys = await generateKeyPair("RS256");
const issuer = "https://identity.test/realms/aurico",
  audience = "aurico-api";
const app = await buildApp(
  db,
  {
    issuer,
    audience,
    clientId: "web",
    clientSecret: "test-only",
    webOrigin: "https://app.test",
    allowLocalHttp: false,
  },
  async (token) =>
    (
      await jwtVerify(token, keys.publicKey, {
        issuer,
        audience,
        algorithms: ["RS256"],
        requiredClaims: ["sub", "exp"],
      })
    ).payload,
);
const tokens: Record<string, string> = {},
  ids: Record<string, string> = {};
let treatmentId = "",
  slotId = "",
  linkId = "";
const symptomId = randomUUID();
const deviceId = randomUUID();
const today = new Date().toISOString().slice(0, 10);
const plan = {
  title: "Acompanhamento de teste automatizado",
  startsOn: today,
  endsOn: today,
  timezone: "UTC",
  symptoms: [
    { id: symptomId, name: "Sintoma definido no teste", min: 0, max: 10 },
  ],
  times: ["00:00", "23:59"],
  instructions: ["Orientação fornecida pelo profissional no teste."],
  pointIds: [],
  followUpAt: null,
};
const call = (
  method: "GET" | "POST" | "PATCH" | "DELETE",
  url: string,
  who: string,
  payload?: unknown,
  headers: Record<string, string> = {},
) =>
  app.inject({
    method,
    url,
    headers: { authorization: `Bearer ${tokens[who]}`, ...headers },
    ...(payload === undefined ? {} : { payload }),
  });
before(async () => {
  await pg.exec(
    await readFile(
      new URL("../migrations/0001_platform.sql", import.meta.url),
      "utf8",
    ),
  );
  await pg.exec(
    await readFile(
      new URL("../migrations/0002_governance.sql", import.meta.url),
      "utf8",
    ),
  );
  for (const [name, role] of [
    ["pro", "professional"],
    ["otherPro", "professional"],
    ["patient", "patient"],
    ["otherPatient", "patient"],
  ]) {
    tokens[name] = await new SignJWT({
      realm_access: { roles: [role] },
      name,
      email: `${name}@example.invalid`,
      email_verified: true,
    })
      .setProtectedHeader({ alg: "RS256" })
      .setIssuer(issuer)
      .setAudience(audience)
      .setSubject(name)
      .setIssuedAt()
      .setExpirationTime("1h")
      .sign(keys.privateKey);
    const response = await call("GET", "/api/v1/me", name);
    assert.equal(response.statusCode, 200, response.body);
    ids[name] = response.json().id;
  }
});
after(async () => {
  await app.close();
  await pg.close();
});
test("unauthenticated and forged tokens are rejected", async () => {
  assert.equal((await app.inject("/api/v1/me")).statusCode, 401);
  assert.equal(
    (
      await app.inject({
        url: "/api/v1/me",
        headers: { authorization: "Bearer forged" },
      })
    ).statusCode,
    401,
  );
  const wrong = await new SignJWT({ realm_access: { roles: ["patient"] } })
    .setProtectedHeader({ alg: "RS256" })
    .setSubject("bad")
    .setIssuer(issuer)
    .setAudience("another-api")
    .setExpirationTime("1h")
    .sign(keys.privateKey);
  assert.equal(
    (
      await app.inject({
        url: "/api/v1/me",
        headers: { authorization: `Bearer ${wrong}` },
      })
    ).statusCode,
    401,
  );
});
test("invitation requires target email and explicit consent; accepts only once", async () => {
  const invite = await call("POST", "/api/v1/patient-invitations", "pro", {
    email: "patient@example.invalid",
  });
  assert.equal(invite.statusCode, 201, invite.body);
  const body = {
    token: invite.json().token,
    consent: true,
    policyVersion: "2026-11-03",
  };
  assert.equal(
    (await call("POST", "/api/v1/patient-links/accept", "otherPatient", body))
      .statusCode,
    404,
  );
  assert.equal(
    (
      await call("POST", "/api/v1/patient-links/accept", "patient", {
        ...body,
        consent: false,
      })
    ).statusCode,
    422,
  );
  const accept = await call(
    "POST",
    "/api/v1/patient-links/accept",
    "patient",
    body,
  );
  assert.equal(accept.statusCode, 200, accept.body);
  linkId = accept.json().id;
  assert.equal(
    (await call("POST", "/api/v1/patient-links/accept", "patient", body))
      .statusCode,
    404,
  );
});
test("creates and publishes a real persisted plan; isolates drafts and patient IDs", async () => {
  assert.equal(
    (
      await call("POST", "/api/v1/treatments", "otherPro", {
        patientId: ids.patient,
        plan,
      })
    ).statusCode,
    404,
  );
  assert.equal(
    (
      await call("POST", "/api/v1/treatments", "patient", {
        patientId: ids.patient,
        plan,
      })
    ).statusCode,
    403,
  );
  const response = await call("POST", "/api/v1/treatments", "pro", {
    patientId: ids.patient,
    plan,
  });
  assert.equal(response.statusCode, 201, response.body);
  treatmentId = response.json().id;
  assert.equal(
    (await call("GET", `/api/v1/treatments/${treatmentId}`, "patient"))
      .statusCode,
    404,
  );
  const publish = await call(
    "POST",
    `/api/v1/treatments/${treatmentId}/publish`,
    "pro",
    { version: 1 },
  );
  assert.equal(publish.statusCode, 200, publish.body);
  const detail = await call(
    "GET",
    `/api/v1/treatments/${treatmentId}`,
    "patient",
  );
  assert.equal(detail.statusCode, 200, detail.body);
  slotId = detail.json().occurrences[0].id;
  assert.equal(detail.json().occurrences.length, 2);
  assert.equal(detail.json().draft, undefined);
  for (const who of ["otherPatient", "otherPro"])
    for (const path of [
      `treatments/${treatmentId}`,
      `treatments/${treatmentId}/symptom-logs`,
      `reports/${treatmentId}`,
    ])
      assert.equal((await call("GET", `/api/v1/${path}`, who)).statusCode, 404);
});
test("offline replay is idempotent and changing its payload conflicts", async () => {
  const op = {
    operationId: randomUUID(),
    kind: "symptom",
    data: {
      id: randomUUID(),
      treatmentId,
      symptomId,
      level: 4,
      occurredAt: new Date().toISOString(),
    },
  };
  const payload = { deviceId, operations: [op] };
  for (let i = 0; i < 2; i++) {
    const r = await call("POST", "/api/v1/sync/push", "patient", payload);
    assert.equal(r.statusCode, 200, r.body);
    assert.equal(r.json().results[0].status, "acknowledged");
  }
  const mismatch = await call("POST", "/api/v1/sync/push", "patient", {
    deviceId,
    operations: [{ ...op, data: { ...op.data, level: 8 } }],
  });
  assert.equal(mismatch.json().results[0].code, "IDEMPOTENCY_MISMATCH");
  assert.equal(
    (
      await call(
        "GET",
        `/api/v1/treatments/${treatmentId}/symptom-logs`,
        "patient",
      )
    ).json().items.length,
    1,
  );
  const correction = {
    operationId: randomUUID(),
    kind: "symptom",
    data: { ...op.data, id: randomUUID(), level: 3, correctionOf: op.data.id },
  };
  assert.equal(
    (
      await call("POST", "/api/v1/sync/push", "patient", {
        deviceId,
        operations: [correction],
      })
    ).json().results[0].status,
    "acknowledged",
  );
  const conflict = await call("POST", "/api/v1/sync/push", "patient", {
    deviceId,
    operations: [
      {
        ...correction,
        operationId: randomUUID(),
        data: { ...correction.data, id: randomUUID() },
      },
    ],
  });
  assert.equal(conflict.json().results[0].code, "ALREADY_CORRECTED");
});
test("invalid scale and foreign resource never enter the outbox receipts", async () => {
  for (const data of [
    { symptomId, level: 11 },
    { symptomId: randomUUID(), level: 1 },
  ]) {
    const op = {
      operationId: randomUUID(),
      kind: "symptom",
      data: {
        id: randomUUID(),
        treatmentId,
        ...data,
        occurredAt: new Date().toISOString(),
      },
    };
    const r = await call("POST", "/api/v1/sync/push", "patient", {
      deviceId,
      operations: [op],
    });
    assert.equal(r.json().results[0].status, "rejected");
    assert.equal(
      (
        await db.query(
          sql`SELECT * FROM sync_operation WHERE operation_id=${op.operationId}`,
        )
      ).length,
      0,
    );
  }
});
test("multiple device confirmations do not inflate adherence", async () => {
  for (let i = 0; i < 2; i++) {
    const r = await call("POST", "/api/v1/sync/push", "patient", {
      deviceId: randomUUID(),
      operations: [
        {
          operationId: randomUUID(),
          kind: "stimulation",
          data: {
            id: randomUUID(),
            treatmentId,
            scheduledId: slotId,
            occurredAt: new Date().toISOString(),
          },
        },
      ],
    });
    assert.equal(r.json().results[0].status, "acknowledged", r.body);
  }
  const report = await call("GET", `/api/v1/reports/${treatmentId}`, "pro");
  assert.equal(report.json().adherence.recorded, 1);
  assert.equal(report.json().adherence.expected, 1);
  assert.equal(report.json().adherence.percentage, 100);
});
test("plan versions are optimistic, preserve history and do not publish drafts early", async () => {
  const patch = await call(
    "PATCH",
    `/api/v1/treatments/${treatmentId}`,
    "pro",
    { version: 2, plan: { ...plan, title: "Plano revisado" } },
  );
  assert.equal(patch.statusCode, 200, patch.body);
  assert.equal(
    (await call("GET", `/api/v1/treatments/${treatmentId}`, "patient")).json()
      .treatment.title,
    plan.title,
  );
  assert.equal(
    (
      await call("PATCH", `/api/v1/treatments/${treatmentId}`, "pro", {
        version: 2,
        plan,
      })
    ).statusCode,
    409,
  );
  assert.equal(
    (
      await call("POST", `/api/v1/treatments/${treatmentId}/publish`, "pro", {
        version: 3,
      })
    ).statusCode,
    200,
  );
  const detail = (
    await call("GET", `/api/v1/treatments/${treatmentId}`, "patient")
  ).json();
  assert.equal(detail.versions.length, 2);
  assert.equal(detail.treatment.title, "Plano revisado");
  assert.equal(detail.occurrences[0].id, slotId);
});
test("pull pagination is account scoped, monotonic and replayable", async () => {
  let cursor = "0",
    count = 0;
  for (let i = 0; i < 20; i++) {
    const r = (
      await call("GET", `/api/v1/sync/pull?cursor=${cursor}&limit=1`, "patient")
    ).json();
    if (r.changes.length) {
      assert.ok(BigInt(r.cursor) > BigInt(cursor));
      count++;
    }
    cursor = r.cursor;
    if (!r.hasMore) break;
  }
  assert.ok(count >= 5);
  assert.equal(
    (await call("GET", "/api/v1/sync/pull", "otherPatient")).json().changes
      .length,
    0,
  );
  assert.equal(
    (await call("GET", `/api/v1/sync/pull?cursor=${cursor}`, "patient")).json()
      .changes.length,
    0,
  );
});
test("PDF is generated locally and private session notes are not exposed to patient", async () => {
  const s = await call(
    "POST",
    `/api/v1/treatments/${treatmentId}/sessions`,
    "pro",
    { occurredAt: new Date().toISOString(), notes: "Private test note" },
  );
  assert.equal(s.statusCode, 201, s.body);
  assert.equal(
    (await call("GET", `/api/v1/treatments/${treatmentId}/sessions`, "patient"))
      .statusCode,
    403,
  );
  const pdf = await call(
    "GET",
    `/api/v1/reports/${treatmentId}?format=pdf`,
    "pro",
  );
  assert.equal(pdf.statusCode, 200, pdf.body.slice(0, 200));
  assert.equal(pdf.rawPayload.subarray(0, 4).toString(), "%PDF");
  assert.equal(pdf.headers["cache-control"], "no-store");
});
test("web mutation requires same origin and secure cookies are not replaced by bearer-like input", async () => {
  const { hash } = await import("../src/modules/auth.js");
  await db.query(
    sql`INSERT INTO web_session(token_hash,user_id,expires_at) VALUES(${hash("session-test")},${ids.pro},now()+interval '1 hour')`,
  );
  const r = await app.inject({
    method: "POST",
    url: "/api/v1/auth/logout",
    headers: {
      cookie: "aurico_session=session-test",
      origin: "https://evil.test",
    },
  });
  assert.equal(r.statusCode, 403);
  assert.equal(
    (
      await app.inject({
        method: "POST",
        url: "/api/v1/auth/logout",
        headers: {
          cookie: "aurico_session=session-test",
          origin: "https://app.test",
        },
      })
    ).statusCode,
    200,
  );
});
test("revocation denies professional reads and pending writes; preserves patient history", async () => {
  assert.equal(
    (await call("DELETE", `/api/v1/patient-links/${linkId}`, "patient"))
      .statusCode,
    200,
  );
  assert.equal(
    (await call("GET", `/api/v1/treatments/${treatmentId}`, "pro")).statusCode,
    404,
  );
  assert.equal(
    (await call("GET", `/api/v1/treatments/${treatmentId}`, "patient"))
      .statusCode,
    200,
  );
  const r = await call("POST", "/api/v1/sync/push", "patient", {
    deviceId,
    operations: [
      {
        operationId: randomUUID(),
        kind: "symptom",
        data: {
          id: randomUUID(),
          treatmentId,
          symptomId,
          level: 2,
          occurredAt: new Date().toISOString(),
        },
      },
    ],
  });
  assert.equal(r.json().results[0].code, "RESOURCE_NOT_FOUND");
  assert.equal(
    (await call("GET", "/api/v1/patients", "pro")).json().items.length,
    0,
  );
});
test("schedule handles DST, duplicates, bounds, and no-denominator adherence", () => {
  const p = planInput.parse({
    ...plan,
    startsOn: "2026-03-08",
    endsOn: "2026-03-08",
    timezone: "America/New_York",
    times: ["02:30", "08:00"],
  });
  assert.equal(schedule(p).length, 1);
  assert.equal(
    schedule({
      ...p,
      startsOn: "2026-11-01",
      endsOn: "2026-11-01",
      times: ["01:30"],
    }).length,
    1,
  );
  assert.equal(adherence([], [], new Date()).percentage, null);
  assert.equal(
    planInput.safeParse({ ...plan, times: ["08:00", "08:00"] }).success,
    false,
  );
  assert.equal(
    planInput.safeParse({ ...plan, endsOn: "2020-01-01" }).success,
    false,
  );
});

test("privacy requests are owned, cancellable, and never delete data automatically", async () => {
  const request = await call("POST", "/api/v1/governance/requests", "patient", {
    kind: "deletion",
    confirmed: true,
  });
  assert.equal(request.statusCode, 201, request.body);
  const id = request.json().id;
  assert.equal(
    (await call("GET", "/api/v1/governance/requests", "otherPatient")).json()
      .items.length,
    0,
  );
  assert.equal(
    (await call("GET", "/api/v1/operations/requests", "pro")).statusCode,
    403,
  );
  assert.equal(
    (
      await call(
        "POST",
        `/api/v1/governance/requests/${id}/cancel`,
        "otherPatient",
        {},
      )
    ).statusCode,
    404,
  );
  assert.equal(
    (
      await call(
        "POST",
        `/api/v1/governance/requests/${id}/cancel`,
        "patient",
        {},
      )
    ).statusCode,
    200,
  );
  assert.ok(
    (await call("GET", "/api/v1/export", "patient")).json().symptomLogs.length >
      0,
  );
});
test("only a separate operations identity can review professional verification", async () => {
  const requested = await call(
    "POST",
    "/api/v1/governance/requests",
    "otherPro",
    {
      kind: "professional_verification",
      registration: "TEST-REG",
      jurisdiction: "Test jurisdiction",
      evidenceReference: "Private test evidence",
    },
  );
  assert.equal(requested.statusCode, 201, requested.body);
  const id = requested.json().id;
  tokens.operator = await new SignJWT({
    realm_access: { roles: ["professional", "aurico-operations"] },
    name: "Operator test",
  })
    .setProtectedHeader({ alg: "RS256" })
    .setIssuer(issuer)
    .setAudience(audience)
    .setSubject("operator-test")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(keys.privateKey);
  const decision = {
    version: 1,
    status: "approved",
    decision: "Evidence verified in isolated integration test.",
    evidenceChecked: true,
  };
  assert.equal(
    (
      await call(
        "POST",
        `/api/v1/operations/requests/${id}/review`,
        "pro",
        decision,
      )
    ).statusCode,
    403,
  );
  assert.equal(
    (
      await call(
        "POST",
        `/api/v1/operations/requests/${id}/review`,
        "operator",
        decision,
      )
    ).statusCode,
    200,
  );
  assert.equal(
    (await call("GET", "/api/v1/me", "otherPro")).json().profile
      .verification_status,
    "verified",
  );
  assert.equal(
    (
      await call(
        "POST",
        `/api/v1/operations/requests/${id}/review`,
        "operator",
        decision,
      )
    ).statusCode,
    409,
  );
});
test("patient snapshots contain session metadata but exclude professional notes", async () => {
  const response = await call(
    "GET",
    `/api/v1/treatments/${treatmentId}`,
    "patient",
  );
  assert.equal(response.statusCode, 200);
  assert.ok(response.json().sessions.length > 0);
  assert.equal(response.json().sessions[0].notes, undefined);
});

test("professional self enrollment is server controlled and refuses existing patient relationships", async () => {
  assert.equal(
    (
      await call("POST", "/api/v1/me/enroll-professional", "patient", {
        confirmed: true,
      })
    ).statusCode,
    409,
  );
  assert.equal(
    (
      await call("POST", "/api/v1/me/enroll-professional", "otherPatient", {
        confirmed: false,
      })
    ).statusCode,
    422,
  );
  assert.equal(
    (
      await call("POST", "/api/v1/me/enroll-professional", "otherPatient", {
        confirmed: true,
        operations_access: true,
      })
    ).statusCode,
    422,
  );
  assert.equal(
    (
      await call("POST", "/api/v1/me/enroll-professional", "otherPatient", {
        confirmed: true,
      })
    ).statusCode,
    200,
  );
  const me = await call("GET", "/api/v1/me", "otherPatient");
  assert.equal(me.statusCode, 200);
  assert.equal(me.json().role, "professional");
  assert.equal(me.json().profile.verification_status, "unverified");
  assert.equal(me.json().operations_access, false);
  assert.equal(
    (await call("GET", "/api/v1/patients", "otherPatient")).json().items.length,
    0,
  );
});
