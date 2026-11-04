"use client";
import { useEffect, useState, type FormEvent } from "react";
type Request = {
  id: string;
  kind: string;
  status: string;
  version: number;
  decision: string | null;
  details: Record<string, unknown>;
  display_name?: string;
};
export function Governance({
  operator,
  professional,
}: {
  operator: boolean;
  professional: boolean;
}) {
  const [items, setItems] = useState<Request[]>([]),
    [queue, setQueue] = useState<Request[]>([]),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false),
    [kind, setKind] = useState("deletion"),
    [registration, setRegistration] = useState(""),
    [jurisdiction, setJurisdiction] = useState(""),
    [reference, setReference] = useState(""),
    [relationship, setRelationship] = useState(""),
    [confirmed, setConfirmed] = useState(false);
  async function call(path: string, method = "GET", body?: unknown) {
    const response = await fetch(`/api/v1${path}`, {
      method,
      credentials: "same-origin",
      cache: "no-store",
      headers: body ? { "Content-Type": "application/json" } : {},
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    const data = await response.json();
    if (!response.ok)
      throw Error(
        (
          {
            OPERATIONS_REQUIRED: "Acesso operacional necessário.",
            SELF_REVIEW_FORBIDDEN: "Você não pode avaliar o próprio pedido.",
            EXTERNAL_REVIEW_REQUIRED:
              "Este pedido exige procedimento externo aprovado. Nenhum dado foi apagado e nenhum acesso de responsável foi concedido.",
            RESOURCE_CONFLICT: "Já existe um pedido aberto deste tipo.",
            VERSION_CONFLICT: "O pedido mudou. Atualize a lista.",
          } as Record<string, string>
        )[data.error?.code] ??
          "Não foi possível concluir. Confira os dados e sua sessão.",
      );
    return data;
  }
  async function load() {
    try {
      setItems((await call("/governance/requests")).items);
      if (operator) setQueue((await call("/operations/requests")).items);
    } catch (e) {
      setError((e as Error).message);
    }
  }
  useEffect(() => {
    void load();
  }, [operator]); // Reads are scoped to the current authenticated account; parent unmounts on logout.
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const body =
        kind === "deletion"
          ? { kind, confirmed }
          : kind === "professional_verification"
            ? { kind, registration, jurisdiction, evidenceReference: reference }
            : { kind, relationship, evidenceReference: reference, confirmed };
      const result = await call("/governance/requests", "POST", body);
      setMessage(
        `Pedido registrado. Protocolo: ${result.id}. A solicitação não executa exclusão nem concede acesso automaticamente.`,
      );
      setConfirmed(false);
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <h1>Privacidade e revisão cadastral</h1>
      <p>
        Consulte seus protocolos e acompanhe a análise. Não envie documentos,
        senhas ou informações clínicas nos campos abaixo.
      </p>
      {error && (
        <div role="alert" className="notice error">
          {error}
        </div>
      )}
      {message && (
        <div role="status" className="notice">
          {message}
        </div>
      )}
      <section className="panel">
        <h2>Novo pedido</h2>
        <form onSubmit={submit}>
          <label>
            Tipo
            <select value={kind} onChange={(e) => setKind(e.target.value)}>
              <option value="deletion">
                Solicitar análise de exclusão dos dados
              </option>
              {professional && (
                <option value="professional_verification">
                  Solicitar verificação do cadastro profissional
                </option>
              )}
              <option value="guardian_review">
                Solicitar revisão de representação legal
              </option>
            </select>
          </label>
          {kind === "professional_verification" ? (
            <>
              <label>
                Registro profissional
                <input
                  required
                  maxLength={120}
                  value={registration}
                  onChange={(e) => setRegistration(e.target.value)}
                />
              </label>
              <label>
                Jurisdição e órgão
                <input
                  required
                  maxLength={120}
                  value={jurisdiction}
                  onChange={(e) => setJurisdiction(e.target.value)}
                />
              </label>
            </>
          ) : kind === "guardian_review" ? (
            <>
              <p>
                A revisão não concede acesso aos dados de outra pessoa. O
                atendimento a menores depende de política aprovada e verificação
                humana.
              </p>
              <label>
                Relação de representação
                <input
                  required
                  maxLength={80}
                  value={relationship}
                  onChange={(e) => setRelationship(e.target.value)}
                />
              </label>
            </>
          ) : (
            <p>
              O responsável analisará identidade, escopo e retenção aplicável.
              Nenhum registro será removido automaticamente. Você pode revogar o
              compartilhamento separadamente.
            </p>
          )}
          {kind !== "deletion" && (
            <label>
              Referência privada para a evidência (sem documento anexado)
              <input
                required
                minLength={3}
                maxLength={500}
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            </label>
          )}
          <label className="checkbox">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(e) => setConfirmed(e.target.checked)}
            />
            Confirmo as informações e solicito a revisão descrita.
          </label>
          <button disabled={!confirmed || busy}>Registrar pedido</button>
        </form>
      </section>
      <section className="panel">
        <h2>Meus protocolos</h2>
        <button className="secondary" onClick={load}>
          Atualizar
        </button>
        {items.map((r) => (
          <article className="item" key={r.id}>
            <div>
              <strong>
                {r.kind} · {r.status}
              </strong>
              <small>{r.id}</small>
              <p>{r.decision}</p>
              {["requested", "needs_information"].includes(r.status) && (
                <button
                  className="secondary"
                  onClick={async () => {
                    try {
                      await call(
                        `/governance/requests/${r.id}/cancel`,
                        "POST",
                        {},
                      );
                      await load();
                    } catch (e) {
                      setError((e as Error).message);
                    }
                  }}
                >
                  Cancelar pedido
                </button>
              )}
            </div>
          </article>
        ))}
        {!items.length && <p>Nenhum pedido registrado.</p>}
      </section>
      {operator && (
        <section className="panel">
          <h2>Fila de revisão operacional</h2>
          <p>
            Verifique evidências na fonte competente. A aprovação de exclusão ou
            representação não é executada por esta interface.
          </p>
          {queue.map((r) => (
            <Review
              key={r.id}
              request={r}
              review={async (status, decision) => {
                try {
                  await call(`/operations/requests/${r.id}/review`, "POST", {
                    version: r.version,
                    status,
                    decision,
                    evidenceChecked: true,
                  });
                  await load();
                } catch (e) {
                  setError((e as Error).message);
                }
              }}
            />
          ))}
        </section>
      )}
    </>
  );
}
function Review({
  request,
  review,
}: {
  request: Request;
  review: (status: string, decision: string) => Promise<void>;
}) {
  const [decision, setDecision] = useState(""),
    [status, setStatus] = useState("under_review"),
    [checked, setChecked] = useState(false),
    [busy, setBusy] = useState(false);
  return (
    <article className="panel">
      <h3>
        {request.display_name} · {request.kind} · {request.status}
      </h3>
      <p>{request.id}</p>
      <pre style={{ whiteSpace: "pre-wrap" }}>
        {JSON.stringify(request.details, null, 2)}
      </pre>
      {!["approved", "denied", "cancelled"].includes(request.status) && (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            try {
              await review(status, decision);
            } finally {
              setBusy(false);
            }
          }}
        >
          <label>
            Decisão
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="under_review">Em análise</option>
              <option value="needs_information">Solicitar complemento</option>
              <option value="denied">Indeferir com justificativa</option>
              {request.kind === "professional_verification" && (
                <option value="approved">Aprovar revisão cadastral</option>
              )}
            </select>
          </label>
          <label>
            Justificativa
            <textarea
              required
              minLength={10}
              maxLength={2000}
              value={decision}
              onChange={(e) => setDecision(e.target.value)}
            />
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={checked}
              onChange={(e) => setChecked(e.target.checked)}
            />
            Verifiquei as evidências e tenho autorização operacional para
            registrar esta decisão.
          </label>
          <button disabled={!checked || busy}>
            Registrar revisão auditada
          </button>
        </form>
      )}
    </article>
  );
}
