# Contrato REST v1

Base: `/api/v1`. Documento OpenAPI em `GET /api/v1/openapi` (sem dados de pacientes). Consultas e respostas de acompanhamento usam `Cache-Control: no-store`.

## Acesso e erros

Android: `Authorization: Bearer <access_token>` com `iss`, `aud=aurico-api`, `sub`, `exp` e papel validado. Web: cookie de sessão HttpOnly. Escritas com cookie exigem Origin igual a `WEB_ORIGIN`. IDs são UUID. Datas de acompanhamento são `YYYY-MM-DD`; instantes são ISO 8601 com fuso. Não enviar senha para esta API.

Erro: `{"error":{"code":"VERSION_CONFLICT","requestId":"req-..."}}`. HTTP 401: identidade inválida; 403: papel, e-mail verificado ou CSRF; 404: recurso inexistente ou não autorizado (mesma resposta); 409: conflito; 422: entrada inválida; 429: limite. Erros não incluem conteúdo de saúde ou detalhes SQL.

Listagens: `page` a partir de 1, `limit` 1–100, padrão 30; `q` até 100 caracteres. Retorno `{items, page, limit}` quando aplicável. Sessões, pontos e painel têm respostas específicas descritas abaixo.

| Endpoint | Acesso e resultado |
|---|---|
| GET `/me` | Identidade interna e perfil; verificação profissional explícita |
| GET `/patients` | Somente profissional; pacientes com vínculo ativo, pesquisa por nome |
| POST `/patient-invitations` | Profissional; `{email}` → `{id, token, expiresInDays}`; token entregue uma vez, sem envio automático de e-mail |
| GET `/patient-links` | Paciente; seus vínculos e identificação do profissional |
| POST `/patient-links/accept` | Paciente com e-mail verificado; `{token, consent:true, policyVersion:"2026-11-03"}` → `{id}` |
| DELETE `/patient-links/:id` | Paciente ou profissional daquele vínculo; revogação lógica, histórico preservado |
| GET `/treatments` | Recursos da identidade autorizada; filtros `patientId`, `status` e `q`; paciente não vê rascunhos |
| POST `/treatments` | Profissional vinculado; `{patientId, plan}` → `{id, version:1}` |
| GET `/treatments/:id` | Snapshot com tratamento, plano publicado, sintomas, ocorrências, retorno, pontos e versões; rascunho apenas ao profissional responsável |
| PATCH `/treatments/:id` | Profissional; `{version, plan}`; conflito se versão mudou; altera apenas rascunho |
| POST `/treatments/:id/publish` | Profissional; `{version}`; nova versão imutável e snapshot para sincronização |
| POST `/treatments/:id/finish` | Profissional; `{}`; cancela ocorrências e retornos futuros |
| GET/POST `/treatments/:id/sessions` | Profissional responsável; POST `{occurredAt, notes?}`; notas profissionais não são compartilhadas com paciente |
| GET `/treatments/:id/symptom-logs` | Paciente dono ou profissional responsável vinculado; eventos e correções paginados |
| GET `/treatments/:id/stimulations` | Mesmo controle; registros referenciando ocorrências |
| POST nos dois endpoints de registros | Mesmo envelope de operação do sync: `{deviceId, operation}`; paciente somente |
| GET/POST `/points` | Biblioteca privada do profissional. POST `{label, source, x, y, reviewed:true}`; coordenadas 0–1 e fonte obrigatória |
| GET `/dashboard` | Profissional; contagens, até dez retornos, dez registros recentes e pacientes sem check-in nos últimos três dias |
| GET `/reports/:treatmentId` | Paciente dono ou profissional responsável; JSON, ou PDF com `format=pdf`; filtros ISO `from`, `to` |
| POST `/sync/push` | Paciente; lote de até 50 operações, recibos individuais |
| GET `/sync/pull` | Paciente; `cursor` decimal e `limit` até 100; `{changes,cursor,hasMore}` |
| GET `/export` | Paciente; exportação pessoal JSON, auditada |
| GET `/auth/login` | Inicia OIDC/PKCE; redirecionamento para o provedor |
| GET `/auth/callback` | Valida state, nonce e PKCE e cria sessão web |
| POST `/auth/logout` | Encerra sessão local da aplicação |

## Plano

```json
{
  "title": "Identificação definida pelo profissional",
  "startsOn": "2026-11-04",
  "endsOn": "2026-11-10",
  "timezone": "America/Sao_Paulo",
  "symptoms": [{"id":"UUID", "name":"Sintoma definido pelo profissional", "min":0, "max":10}],
  "times": ["08:30", "17:00"],
  "instructions": ["Texto revisado e aprovado pelo profissional responsável"],
  "pointIds": [],
  "followUpAt": null
}
```

Este exemplo documenta o formato; não é um protocolo clínico nem um registro de paciente. UUIDs reais são gerados no cliente. Máximo 20 sintomas, 20 instruções (2.000 caracteres cada), 30 pontos e oito horários; duração 1–366 dias. Escalas inteiras entre 0 e 100, com máximo maior que mínimo. IDs/horários duplicados são recusados. Início/fuso não mudam após publicação. Definições publicadas de sintomas são imutáveis; nova escala exige novo sintoma UUID.

## Operações offline

```text
{deviceId: UUID, operations: [
  {operationId: UUID, kind: "symptom", data: {
    id: UUID, treatmentId: UUID, symptomId: UUID,
    level: inteiro, note: texto|null, occurredAt: ISO,
    correctionOf: UUID|null, sessionId: UUID|null
  }},
  {operationId: UUID, kind: "stimulation", data: {
    id: UUID, treatmentId: UUID, scheduledId: UUID, occurredAt: ISO
  }}
]}
```

Cada resultado contém `operationId`, `status:acknowledged` e `record` canônico, ou `status:rejected` e `code`. Repetir uma operação mantém seu ID e corpo; não gerar outro ID a cada retry. O servidor conserva recibo por usuário e operação. Dados fora do tratamento, escala inválida, ocorrência futura/cancelada, vínculo revogado e correção concorrente são recusados. Registros recusados devem permanecer locais para revisão.

## Governança

`GET/POST /governance/requests`: protocolos da própria conta. Tipos: `deletion` com confirmação explícita; `professional_verification` com registro, jurisdição e referência privada de evidência; `guardian_review` com relação, referência de evidência e confirmação. Pedidos abertos duplicados são recusados. `POST /governance/requests/:id/cancel` permite cancelar pedidos próprios ainda não decididos.

`GET /operations/requests` e `POST /operations/requests/:id/review` exigem papel OIDC `aurico-operations`. Revisão usa versão otimista, justificativa e confirmação de evidência verificada. É proibida autoaprovação. Somente verificação profissional pode receber aprovação nessa interface; exclusão e representação exigem procedimento externo aprovado. O snapshot de tratamento agora inclui somente ID/data de sessões, sem notas privadas.
