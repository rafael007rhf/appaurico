import { z } from "zod";
import { planInput } from "./modules/plans.js";
import { operation } from "./modules/records.js";
const operations: Record<string, string[]> = {
  "/governance/requests": ["get", "post"],
  "/governance/requests/{id}/cancel": ["post"],
  "/operations/requests": ["get"],
  "/operations/requests/{id}/review": ["post"],
  "/me": ["get"],
  "/me/enroll-professional": ["post"],
  "/patients": ["get"],
  "/patient-invitations": ["get", "post"],
  "/patient-invitations/{id}": ["delete"],
  "/patient-links": ["get"],
  "/patient-links/accept": ["post"],
  "/patient-links/{id}": ["delete"],
  "/treatments": ["get", "post"],
  "/treatments/{id}": ["get", "patch"],
  "/treatments/{id}/publish": ["post"],
  "/treatments/{id}/finish": ["post"],
  "/treatments/{id}/sessions": ["get", "post"],
  "/treatments/{id}/symptom-logs": ["get", "post"],
  "/treatments/{id}/stimulations": ["get", "post"],
  "/dashboard": ["get"],
  "/points": ["get", "post"],
  "/reports/{treatmentId}": ["get"],
  "/sync/push": ["post"],
  "/sync/pull": ["get"],
  "/export": ["get"],
  "/auth/logout": ["post"],
};
const bodies: Record<string, unknown> = {
  "post /me/enroll-professional": {
    type: "object",
    additionalProperties: false,
    required: ["confirmed"],
    properties: { confirmed: { const: true } },
  },
  "post /treatments": {
    type: "object",
    required: ["patientId", "plan"],
    properties: {
      patientId: { type: "string", format: "uuid" },
      plan: { $ref: "#/components/schemas/Plan" },
    },
  },
  "patch /treatments/{id}": {
    type: "object",
    required: ["version", "plan"],
    properties: {
      version: { type: "integer" },
      plan: { $ref: "#/components/schemas/Plan" },
    },
  },
  "post /treatments/{id}/publish": {
    type: "object",
    required: ["version"],
    properties: { version: { type: "integer", minimum: 1 } },
  },
  "post /sync/push": {
    type: "object",
    required: ["deviceId", "operations"],
    properties: {
      deviceId: { type: "string", format: "uuid" },
      operations: {
        type: "array",
        minItems: 1,
        maxItems: 50,
        items: { $ref: "#/components/schemas/SyncOperation" },
      },
    },
  },
  "post /patient-invitations": {
    type: "object",
    required: ["email"],
    properties: { email: { type: "string", format: "email" } },
  },
  "post /patient-links/accept": {
    type: "object",
    required: ["token", "consent", "policyVersion"],
    properties: {
      token: { type: "string" },
      consent: { const: true },
      policyVersion: { const: "2026-11-03" },
    },
  },
};
export const openapi = {
  openapi: "3.1.0",
  info: {
    title: "AURICO Platform",
    version: "3.0.0",
    description:
      "Authorization is enforced by role, owner and active link. Full field and conflict semantics: docs/API.md.",
  },
  servers: [{ url: "/api/v1" }],
  security: [{ bearer: [] }, { session: [] }],
  paths: Object.fromEntries(
    Object.entries(operations).map(([path, methods]) => [
      path,
      Object.fromEntries(
        methods.map((method) => [
          method,
          {
            operationId: `${method}_${path.replaceAll(/[^a-zA-Z0-9]/g, "_")}`,
            parameters: [...path.matchAll(/\{(.*?)\}/g)].map((m) => ({
              name: m[1],
              in: "path",
              required: true,
              schema: { type: "string", format: "uuid" },
            })),
            ...(bodies[`${method} ${path}`]
              ? {
                  requestBody: {
                    required: true,
                    content: {
                      "application/json": {
                        schema: bodies[`${method} ${path}`],
                      },
                    },
                  },
                }
              : {}),
            responses: {
              "200": {
                description:
                  "Authorized response; see API.md for resource shape",
              },
              "201": { description: "Resource created" },
              "401": { description: "Authentication required" },
              "403": {
                description: "Role, verified email or CSRF check failed",
              },
              "404": { description: "Unavailable or unauthorized resource" },
              "409": { description: "Version or idempotency conflict" },
              "422": { description: "Invalid input" },
              "429": { description: "Rate limit" },
            },
          },
        ]),
      ),
    ]),
  ),
  components: {
    securitySchemes: {
      bearer: { type: "http", scheme: "bearer", bearerFormat: "JWT" },
      session: { type: "apiKey", in: "cookie", name: "aurico_session" },
    },
    schemas: {
      Plan: z.toJSONSchema(planInput),
      SyncOperation: z.toJSONSchema(operation),
    },
  },
};
