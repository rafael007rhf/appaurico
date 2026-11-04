"use client";
import { Governance } from "./governance";
import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
type User = {
  operations_access?: boolean;
  id: string;
  display_name: string;
  role: "patient" | "professional";
  profile: { verification_status: string } | null;
};
type Patient = { id: string; display_name: string; link_id: string };
type Treatment = {
  id: string;
  title: string;
  patient_name: string;
  status: string;
  version: number;
  starts_on: string;
  ends_on: string;
  timezone: string;
};
type Symptom = { id: string; name: string; min: number; max: number };
type Plan = {
  title: string;
  startsOn: string;
  endsOn: string;
  timezone: string;
  symptoms: Symptom[];
  times: string[];
  instructions: string[];
  pointIds: string[];
  followUpAt: string | null;
};
type Point = {
  id: string;
  label: string;
  source: string;
  x: number;
  y: number;
};
type RecordLog = {
  id: string;
  name: string;
  symptom_id: string;
  level: number;
  note: string | null;
  occurred_at: string;
  correction_of: string | null;
};
type Report = {
  patient: string;
  generatedAt: string;
  symptomLogs: RecordLog[];
  adherence: { recorded: number; expected: number; percentage: number | null };
};
class RequestError extends Error {
  constructor(
    public status: number,
    public code: string,
  ) {
    super(code);
  }
}
const messages: Record<string, string> = {
  UNAUTHENTICATED: "Sua sessão expirou. Entre novamente.",
  RESOURCE_NOT_FOUND: "O registro não está disponível para esta conta.",
  VERSION_CONFLICT:
    "O plano foi alterado. Atualize a página antes de continuar.",
  VALIDATION_ERROR: "Revise os campos e tente novamente.",
  INVITATION_UNAVAILABLE:
    "Convite expirado, já utilizado ou destinado a outro e-mail.",
  VERIFIED_EMAIL_REQUIRED:
    "Confirme seu e-mail no provedor de acesso antes de aceitar.",
  CSRF_REJECTED: "Atualize a página e tente novamente.",
  SERVICE_UNAVAILABLE:
    "O serviço está indisponível. Verifique a configuração e tente novamente.",
  RATE_LIMITED: "Muitas solicitações. Aguarde um minuto.",
  TREATMENT_NOT_ACTIVE: "O acompanhamento não está ativo.",
  START_AND_TIMEZONE_IMMUTABLE:
    "Após a publicação, início e fuso horário devem ser preservados.",
  SYMPTOM_DEFINITION_IMMUTABLE:
    "Sintomas publicados preservam nome e escala. Adicione outro sintoma para usar uma nova definição.",
};
async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    method,
    credentials: "same-origin",
    cache: "no-store",
    signal,
    headers: body === undefined ? {} : { "Content-Type": "application/json" },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new RequestError(
      response.status,
      data?.error?.code ?? "SERVICE_UNAVAILABLE",
    );
  }
  return response.json();
}
const explain = (e: unknown) =>
  e instanceof RequestError
    ? (messages[e.code] ??
      "Não foi possível concluir. Verifique os dados e tente novamente.")
    : "Não foi possível conectar. Tente novamente.";
function useData<T>(path: string) {
  const [data, setData] = useState<T | null>(null),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision((n) => n + 1), []);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setData(null);
    api<T>(path, "GET", undefined, controller.signal)
      .then(setData)
      .catch((e) => {
        if (!controller.signal.aborted) setError(explain(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [path, revision]);
  useEffect(() => {
    if (
      !["/dashboard", "/patients", "/treatments", "/reports"].some((prefix) =>
        path.startsWith(prefix),
      )
    )
      return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible")
        api<T>(path)
          .then(setData)
          .catch((e) => setError(explain(e)));
    }, 30000);
    return () => window.clearInterval(timer);
  }, [path]);
  return { data, error, loading, reload };
}
const date = (value: string) =>
  new Date(value).toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
const status = (s: string) =>
  ({ active: "Em andamento", draft: "Rascunho", finished: "Encerrado" })[s] ??
  s;
function Notice({
  children,
  error = false,
}: {
  children: ReactNode;
  error?: boolean;
}) {
  return (
    <div
      className={`notice ${error ? "error" : ""}`}
      role={error ? "alert" : "status"}
    >
      {children}
    </div>
  );
}
function Empty({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="empty">
      <span className="empty-symbol" aria-hidden="true">
        ◎
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
function Loading() {
  return (
    <div className="loading" role="status">
      Carregando seu acompanhamento…
    </div>
  );
}
function Brand() {
  return (
    <div className="brand">
      <svg viewBox="0 0 42 42" fill="none" aria-hidden="true">
        <path
          d="M29 26c7-6 8-17-1-21C17 0 7 10 10 20c1 5 5 4 6 10 2 9 12 8 13-4Z"
          stroke="currentColor"
          strokeWidth="2.4"
        />
        <path
          d="M18 23c-5-9 4-17 10-10 4 5-3 8-5 12"
          stroke="currentColor"
          strokeWidth="2"
        />
        <circle cx="23" cy="17" r="3" fill="#D98A3D" />
      </svg>
      <span>
        AURICO<small>ACOMPANHAMENTO</small>
      </span>
    </div>
  );
}
export function Platform() {
  const me = useData<User>("/me"),
    [tab, setTab] = useState("overview"),
    [selected, setSelected] = useState<string | null>(null);
  if (me.loading)
    return (
      <main className="welcome">
        <Brand />
        <Loading />
      </main>
    );
  if (!me.data)
    return (
      <main className="welcome">
        <section>
          <Brand />
          <span className="eyebrow">AURICO PROFISSIONAL</span>
          <h1>
            O cuidado continua
            <br />
            entre as sessões.
          </h1>
          <p className="lead">
            Organize planos, acompanhe os registros e mantenha uma visão clara
            de cada acompanhamento.
          </p>
          <a className="button" href="/api/v1/auth/login">
            Entrar com segurança <span aria-hidden="true">↗</span>
          </a>
          <p className="muted">
            Cadastro e recuperação de acesso no provedor de identidade.
          </p>
          <button className="link" onClick={me.reload}>
            Verificar conexão
          </button>
          {me.error && (
            <Notice>
              {me.error} O acesso depende da API e do provedor de identidade
              configurados.
            </Notice>
          )}
        </section>
        <aside className="welcome-art">
          <span className="eyebrow">PRESENÇA EM CADA ETAPA</span>
          <h2>
            Um plano claro.
            <br />
            Um histórico que
            <br />
            acompanha você.
          </h2>
          <div className="orbit">
            <span>◎</span>
            <i />
            <i />
            <i />
          </div>
          <p>
            Orientações definidas pelo profissional.
            <br />
            Registros feitos pelo paciente.
            <br />
            Compartilhamento com consentimento.
          </p>
        </aside>
      </main>
    );
  const user = me.data;
  async function logout() {
    try {
      await api("/auth/logout", "POST", {});
      setSelected(null);
      me.reload();
    } catch {
      me.reload();
    }
  }
  const items = [
    ["overview", "◫", "Visão geral"],
    ["patients", "◎", "Pacientes"],
    ["treatments", "▤", "Tratamentos"],
    ["points", "⌖", "Biblioteca de pontos"],
    ["governance", "◇", "Privacidade e cadastro"],
  ];
  return (
    <div className="shell">
      <aside className="sidebar">
        <Brand />
        <div className="workspace-label">
          ÁREA {user.role === "professional" ? "PROFISSIONAL" : "DO PACIENTE"}
        </div>
        <nav aria-label="Navegação principal">
          {(user.role === "professional"
            ? items
            : [
                ["sharing", "◎", "Compartilhamento"],
                ["governance", "◇", "Privacidade e cadastro"],
              ]
          ).map(([id, icon, label]) => (
            <button
              key={id}
              className={tab === id ? "active" : ""}
              onClick={() => {
                setTab(id);
                setSelected(null);
              }}
              aria-current={tab === id ? "page" : undefined}
            >
              <span aria-hidden="true">{icon}</span>
              {label}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <p>
            O AURICO apoia o acompanhamento e não substitui decisões clínicas.
          </p>
          <div className="account">
            <span className="avatar">{user.display_name.charAt(0)}</span>
            <div>
              <strong>{user.display_name}</strong>
              <small>
                {user.role === "professional"
                  ? "Conta profissional"
                  : "Conta paciente"}
              </small>
            </div>
          </div>
          <button className="link" onClick={logout}>
            Sair desta sessão
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <span>
            Acompanhamento /{" "}
            <strong>
              {selected
                ? "Plano individual"
                : (items.find((i) => i[0] === tab)?.[2] ?? "Compartilhamento")}
            </strong>
          </span>
          <span className="badge">
            {user.role === "professional"
              ? user.profile?.verification_status === "verified"
                ? "Cadastro verificado"
                : "Cadastro não verificado"
              : "Acesso pessoal"}
          </span>
        </header>
        <main id="main-content">
          {tab === "governance" ? (
            <Governance
              operator={user.operations_access === true}
              professional={user.role === "professional"}
            />
          ) : user.role === "patient" ? (
            <>
              <Sharing />
              <section className="panel">
                <h2>Usar o AURICO como profissional</h2>
                <p>
                  Uma conta sem acompanhamentos de paciente pode abrir a área
                  profissional. O cadastro permanece não verificado até revisão.
                  Esta alteração exige uma conta separada se você já usa a
                  plataforma como paciente.
                </p>
                <button
                  onClick={async () => {
                    try {
                      await api("/me/enroll-professional", "POST", {
                        confirmed: true,
                      });
                      me.reload();
                    } catch (e) {
                      window.alert(explain(e));
                    }
                  }}
                >
                  Criar meu perfil profissional
                </button>
              </section>
            </>
          ) : selected ? (
            <TreatmentDetail id={selected} back={() => setSelected(null)} />
          ) : tab === "overview" ? (
            <Dashboard
              open={setSelected}
              navigate={setTab}
              name={user.display_name}
            />
          ) : tab === "patients" ? (
            <Patients open={setSelected} />
          ) : tab === "points" ? (
            <Points />
          ) : (
            <Treatments open={setSelected} />
          )}
        </main>
        <footer>
          Dados de acompanhamento • Acesso restrito por vínculo e consentimento
        </footer>
      </div>
    </div>
  );
}
function Dashboard({
  open,
  navigate,
  name,
}: {
  open: (id: string) => void;
  navigate: (tab: string) => void;
  name: string;
}) {
  const { data, error, loading, reload } = useData<{
    active_patients: number;
    active_treatments: number;
    drafts: number;
    returns: {
      id: string;
      title: string;
      display_name: string;
      scheduled_at: string;
      treatment_id: string;
    }[];
    recent: {
      id: string;
      display_name: string;
      symptom: string;
      level: number;
      occurred_at: string;
      treatment_id: string;
    }[];
    missing: { id: string; display_name: string }[];
  }>("/dashboard");
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">VISÃO GERAL</span>
          <h1>Olá, {name.split(" ")[0]}.</h1>
          <p>Um olhar atento para a continuidade do cuidado.</p>
        </div>
        <button onClick={() => navigate("patients")}>
          Gerenciar pacientes <span aria-hidden="true">↗</span>
        </button>
      </div>
      {error && (
        <Notice error>
          {error}
          <button onClick={reload}>Tentar novamente</button>
        </Notice>
      )}
      {loading ? (
        <Loading />
      ) : (
        data && (
          <>
            <div className="metrics">
              {[
                [
                  data.active_patients,
                  "Pacientes vinculados",
                  "Compartilhamento autorizado",
                ],
                [
                  data.active_treatments,
                  "Tratamentos em andamento",
                  "Planos publicados",
                ],
                [
                  data.returns.length,
                  "Próximos retornos",
                  "Até 10 próximos agendamentos",
                ],
                [data.drafts, "Planos em rascunho", "Aguardando publicação"],
              ].map(([value, label, detail]) => (
                <article className="metric" key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                  <small>{detail}</small>
                </article>
              ))}
            </div>
            <div className="columns">
              <section className="panel">
                <div className="section-title">
                  <h2>Registros recentes</h2>
                  <span className="badge">Informados pelo paciente</span>
                </div>
                {data.recent.length ? (
                  <div className="rows">
                    {data.recent.map((r) => (
                      <button
                        className="record-row"
                        key={r.id}
                        onClick={() => open(r.treatment_id)}
                      >
                        <span className="avatar">
                          {r.display_name.charAt(0)}
                        </span>
                        <span>
                          <strong>{r.display_name}</strong>
                          <small>
                            {r.symptom} · {date(r.occurred_at)}
                          </small>
                        </span>
                        <span className="value">{r.level}</span>
                      </button>
                    ))}
                  </div>
                ) : (
                  <Empty title="Os primeiros registros aparecem aqui">
                    Convide um paciente e publique um plano para começar o
                    acompanhamento.
                  </Empty>
                )}
              </section>
              <section className="panel">
                <h2>Próximos retornos</h2>
                {data.returns.length ? (
                  data.returns.map((r) => (
                    <button
                      className="return-row"
                      key={r.id}
                      onClick={() => open(r.treatment_id)}
                    >
                      <span className="calendar-day">
                        {new Date(r.scheduled_at).getDate()}
                      </span>
                      <span>
                        <strong>{r.display_name}</strong>
                        <small>{date(r.scheduled_at)}</small>
                      </span>
                    </button>
                  ))
                ) : (
                  <Empty title="Agenda livre">
                    Configure o retorno no plano do paciente.
                  </Empty>
                )}
              </section>
            </div>
            <section className="panel">
              <h2>Sem check-in nos últimos 3 dias</h2>
              <p className="muted">
                A ausência de registros não indica agravamento clínico.
              </p>
              {data.missing.length ? (
                <div className="chips">
                  {data.missing.map((p) => (
                    <span key={p.id} className="badge">
                      {p.display_name}
                    </span>
                  ))}
                </div>
              ) : (
                <p>Nenhum acompanhamento nessa condição.</p>
              )}
            </section>
          </>
        )
      )}
    </>
  );
}
function Patients({ open }: { open: (id: string) => void }) {
  const [search, setSearch] = useState(""),
    [query, setQuery] = useState(""),
    [chosen, setChosen] = useState<Patient | null>(null),
    [email, setEmail] = useState(""),
    [token, setToken] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const list = useData<{ items: Patient[] }>(
    `/patients?q=${encodeURIComponent(query)}`,
  );
  async function invite(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setToken("");
    try {
      const result = await api<{ token: string }>(
        "/patient-invitations",
        "POST",
        { email },
      );
      setToken(result.token);
      setEmail("");
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  if (chosen)
    return (
      <>
        <button className="link" onClick={() => setChosen(null)}>
          ← Todos os pacientes
        </button>
        <h1>{chosen.display_name}</h1>
        <Treatments open={open} patient={chosen} />
        <Revoke
          linkId={chosen.link_id}
          done={() => {
            setChosen(null);
            list.reload();
          }}
        />
      </>
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">VÍNCULOS AUTORIZADOS</span>
          <h1>Pacientes</h1>
          <p>Cada acompanhamento começa com consentimento.</p>
        </div>
      </div>
      <div className="columns">
        <section className="panel">
          <form
            className="search"
            onSubmit={(e) => {
              e.preventDefault();
              setQuery(search);
            }}
          >
            <label className="grow">
              Pesquisar paciente
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Nome do paciente"
                maxLength={100}
              />
            </label>
            <button>Pesquisar</button>
          </form>
          {list.loading ? (
            <Loading />
          ) : list.error ? (
            <Notice error>{list.error}</Notice>
          ) : list.data?.items.length ? (
            list.data.items.map((p) => (
              <button
                className="record-row"
                key={p.id}
                onClick={() => setChosen(p)}
              >
                <span className="avatar">{p.display_name.charAt(0)}</span>
                <strong className="grow">{p.display_name}</strong>
                <span>Ver acompanhamento →</span>
              </button>
            ))
          ) : (
            <Empty title="Nenhum paciente vinculado">
              O paciente aparece aqui após aceitar o convite com seu e-mail
              verificado.
            </Empty>
          )}
        </section>
        <section className="panel">
          <h2>Convidar paciente</h2>
          <p>
            O convite é válido por 7 dias e só pode ser aceito pela conta deste
            e-mail.
          </p>
          <form onSubmit={invite}>
            <label>
              E-mail do paciente
              <input
                required
                type="email"
                maxLength={254}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <button disabled={busy}>
              {busy ? "Criando…" : "Criar convite"}
            </button>
          </form>
          {error && <Notice error>{error}</Notice>}
          {token && (
            <Notice>
              <strong>Convite criado</strong>
              <p>
                Entregue este código ao paciente por um canal autorizado. Nenhum
                e-mail foi enviado automaticamente.
              </p>
              <label>
                Código de uso único
                <textarea readOnly value={token} />
              </label>
            </Notice>
          )}
        </section>
      </div>
    </>
  );
}
function Revoke({ linkId, done }: { linkId: string; done: () => void }) {
  const [confirm, setConfirm] = useState(false),
    [error, setError] = useState("");
  return (
    <section className="panel">
      <h2>Gerenciar compartilhamento</h2>
      <p>
        A revogação impede novos acessos do profissional. O histórico do
        paciente é preservado.
      </p>
      {confirm ? (
        <>
          <button
            className="danger"
            onClick={async () => {
              try {
                await api(`/patient-links/${linkId}`, "DELETE");
                done();
              } catch (e) {
                setError(explain(e));
              }
            }}
          >
            Confirmar revogação
          </button>
          <button className="secondary" onClick={() => setConfirm(false)}>
            Cancelar
          </button>
        </>
      ) : (
        <button className="secondary" onClick={() => setConfirm(true)}>
          Revogar vínculo
        </button>
      )}
      {error && <Notice error>{error}</Notice>}
    </section>
  );
}
function Treatments({
  open,
  patient,
}: {
  open: (id: string) => void;
  patient?: Patient;
}) {
  const [filter, setFilter] = useState(""),
    [creating, setCreating] = useState(false),
    [page, setPage] = useState(1);
  const { data, error, loading, reload } = useData<{ items: Treatment[] }>(
    `/treatments?page=${page}${patient ? `&patientId=${patient.id}` : ""}${filter ? `&status=${filter}` : ""}`,
  );
  if (creating && patient)
    return (
      <PlanEditor
        patientId={patient.id}
        done={(id) => {
          setCreating(false);
          open(id);
        }}
        cancel={() => setCreating(false)}
      />
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{patient ? "Acompanhamentos" : "Tratamentos"}</h1>
          <p>Planos, sessões e histórico em um só lugar.</p>
        </div>
        {patient && (
          <button onClick={() => setCreating(true)}>+ Criar tratamento</button>
        )}
      </div>
      <section className="panel">
        <div className="toolbar">
          <label>
            Estado
            <select
              value={filter}
              onChange={(e) => {
                setFilter(e.target.value);
                setPage(1);
              }}
            >
              <option value="">Todos</option>
              <option value="active">Em andamento</option>
              <option value="draft">Rascunhos</option>
              <option value="finished">Encerrados</option>
            </select>
          </label>
          <button className="secondary" onClick={reload}>
            Atualizar
          </button>
        </div>
        {loading ? (
          <Loading />
        ) : error ? (
          <Notice error>{error}</Notice>
        ) : data?.items.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Tratamento</th>
                  <th>Paciente</th>
                  <th>Período</th>
                  <th>Estado</th>
                  <th>
                    <span className="sr-only">Abrir</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong>{t.title}</strong>
                    </td>
                    <td>{t.patient_name}</td>
                    <td>
                      {t.starts_on} — {t.ends_on}
                    </td>
                    <td>
                      <span className={`badge ${t.status}`}>
                        {status(t.status)}
                      </span>
                    </td>
                    <td>
                      <button className="link" onClick={() => open(t.id)}>
                        Abrir →
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty title="Nenhum tratamento neste filtro">
            Selecione um paciente vinculado para criar o primeiro plano.
          </Empty>
        )}
        <div className="pagination">
          <button
            className="secondary"
            disabled={page === 1}
            onClick={() => setPage((p) => p - 1)}
          >
            Anterior
          </button>
          <span>Página {page}</span>
          <button
            className="secondary"
            disabled={(data?.items.length ?? 0) < 30}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima
          </button>
        </div>
      </section>
    </>
  );
}
function PlanEditor({
  patientId,
  initial,
  treatment,
  done,
  cancel,
}: {
  patientId?: string;
  initial?: Plan;
  treatment?: Treatment;
  done: (id: string) => void;
  cancel: () => void;
}) {
  const [plan, setPlan] = useState<Plan>(
      () =>
        initial ?? {
          title: "",
          startsOn: "",
          endsOn: "",
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
          symptoms: [],
          times: [],
          instructions: [],
          pointIds: [],
          followUpAt: null,
        },
    ),
    [symptomName, setSymptomName] = useState(""),
    [max, setMax] = useState(10),
    [time, setTime] = useState(""),
    [instruction, setInstruction] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const points = useData<{ items: Point[] }>("/points");
  async function save(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const result = await api<{ id: string }>(
        treatment ? `/treatments/${treatment.id}` : "/treatments",
        treatment ? "PATCH" : "POST",
        treatment ? { version: treatment.version, plan } : { patientId, plan },
      );
      done(result.id);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="panel editor">
      <div className="section-title">
        <h2>{treatment ? "Editar rascunho" : "Novo tratamento"}</h2>
        <button className="link" onClick={cancel}>
          Cancelar
        </button>
      </div>
      <p>
        Salvar cria um rascunho. Revise os dados antes de publicar para o
        paciente.
      </p>
      <form onSubmit={save}>
        <label>
          Identificação do acompanhamento
          <input
            required
            maxLength={160}
            value={plan.title}
            onChange={(e) => setPlan({ ...plan, title: e.target.value })}
          />
        </label>
        <div className="form-grid">
          <label>
            Início
            <input
              required
              type="date"
              value={plan.startsOn}
              disabled={treatment?.status === "active"}
              onChange={(e) => setPlan({ ...plan, startsOn: e.target.value })}
            />
          </label>
          <label>
            Fim previsto
            <input
              required
              type="date"
              min={plan.startsOn}
              value={plan.endsOn}
              onChange={(e) => setPlan({ ...plan, endsOn: e.target.value })}
            />
          </label>
          <label>
            Fuso horário
            <input
              required
              value={plan.timezone}
              disabled={treatment?.status === "active"}
              onChange={(e) => setPlan({ ...plan, timezone: e.target.value })}
            />
          </label>
          <label>
            Retorno (horário deste navegador)
            <input
              type="datetime-local"
              value={
                plan.followUpAt
                  ? new Date(
                      new Date(plan.followUpAt).getTime() -
                        new Date(plan.followUpAt).getTimezoneOffset() * 60000,
                    )
                      .toISOString()
                      .slice(0, 16)
                  : ""
              }
              onChange={(e) =>
                setPlan({
                  ...plan,
                  followUpAt: e.target.value
                    ? new Date(e.target.value).toISOString()
                    : null,
                })
              }
            />
          </label>
        </div>
        <fieldset>
          <legend>Sintomas acompanhados</legend>
          {plan.symptoms.map((s) => (
            <div className="item" key={s.id}>
              {s.name} · Escala {s.min}–{s.max}
              <button
                type="button"
                className="link"
                onClick={() =>
                  setPlan({
                    ...plan,
                    symptoms: plan.symptoms.filter((x) => x.id !== s.id),
                  })
                }
              >
                Remover
              </button>
            </div>
          ))}
          <div className="form-grid">
            <label>
              Nome do sintoma
              <input
                value={symptomName}
                maxLength={120}
                onChange={(e) => setSymptomName(e.target.value)}
              />
            </label>
            <label>
              Limite superior da escala
              <input
                type="number"
                min={1}
                max={100}
                value={max}
                onChange={(e) => setMax(Number(e.target.value))}
              />
            </label>
          </div>
          <button
            type="button"
            className="secondary"
            disabled={
              !symptomName.trim() ||
              max < 1 ||
              max > 100 ||
              plan.symptoms.length >= 20
            }
            onClick={() => {
              setPlan({
                ...plan,
                symptoms: [
                  ...plan.symptoms,
                  {
                    id: crypto.randomUUID(),
                    name: symptomName.trim(),
                    min: 0,
                    max,
                  },
                ],
              });
              setSymptomName("");
            }}
          >
            Adicionar sintoma
          </button>
        </fieldset>
        <fieldset>
          <legend>Horários definidos pelo profissional</legend>
          <div className="chips">
            {plan.times.map((t) => (
              <button
                key={t}
                type="button"
                className="secondary"
                onClick={() =>
                  setPlan({ ...plan, times: plan.times.filter((x) => x !== t) })
                }
                aria-label={`Remover horário ${t}`}
              >
                {t} ×
              </button>
            ))}
          </div>
          <label>
            Horário
            <input
              type="time"
              value={time}
              onChange={(e) => setTime(e.target.value)}
            />
          </label>
          <button
            type="button"
            className="secondary"
            disabled={
              !time || plan.times.includes(time) || plan.times.length >= 8
            }
            onClick={() => {
              setPlan({ ...plan, times: [...plan.times, time].sort() });
              setTime("");
            }}
          >
            Adicionar horário
          </button>
        </fieldset>
        <fieldset>
          <legend>Orientações aprovadas</legend>
          {plan.instructions.map((text, i) => (
            <div className="item" key={i}>
              <p>{text}</p>
              <button
                type="button"
                className="link"
                onClick={() =>
                  setPlan({
                    ...plan,
                    instructions: plan.instructions.filter((_, n) => n !== i),
                  })
                }
              >
                Remover
              </button>
            </div>
          ))}
          <label>
            Orientação
            <textarea
              maxLength={2000}
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
            />
          </label>
          <button
            type="button"
            className="secondary"
            disabled={!instruction.trim() || plan.instructions.length >= 20}
            onClick={() => {
              setPlan({
                ...plan,
                instructions: [...plan.instructions, instruction.trim()],
              });
              setInstruction("");
            }}
          >
            Adicionar orientação
          </button>
        </fieldset>
        <fieldset>
          <legend>Pontos revisados da sua biblioteca</legend>
          {points.data?.items.map((p) => (
            <label className="checkbox" key={p.id}>
              <input
                type="checkbox"
                checked={plan.pointIds.includes(p.id)}
                onChange={(e) =>
                  setPlan({
                    ...plan,
                    pointIds: e.target.checked
                      ? [...plan.pointIds, p.id]
                      : plan.pointIds.filter((id) => id !== p.id),
                  })
                }
              />
              {p.label}
              <small>{p.source}</small>
            </label>
          ))}
          {!points.data?.items.length && (
            <p>
              Não há pontos revisados. Cadastre-os na biblioteca; o AURICO não
              fornece posições ou indicações clínicas automáticas.
            </p>
          )}
        </fieldset>
        {error && <Notice error>{error}</Notice>}
        <button
          disabled={
            busy ||
            !plan.symptoms.length ||
            !plan.times.length ||
            !plan.instructions.length
          }
        >
          {busy ? "Salvando…" : "Salvar rascunho para revisão"}
        </button>
      </form>
    </section>
  );
}
function Chart({ logs }: { logs: RecordLog[] }) {
  if (logs.length < 2)
    return <p className="muted">Dados insuficientes para traçar evolução.</p>;
  const max = Math.max(...logs.map((l) => l.level), 1),
    minT = new Date(logs[0].occurred_at).getTime(),
    maxT = new Date(logs.at(-1)!.occurred_at).getTime();
  const coordinates = logs
    .map(
      (l) =>
        `${30 + ((new Date(l.occurred_at).getTime() - minT) / Math.max(1, maxT - minT)) * 640},${155 - (l.level / max) * 120}`,
    )
    .join(" ");
  return (
    <svg
      className="chart"
      viewBox="0 0 700 200"
      role="img"
      aria-label={`Evolução de ${logs[0].name}, ${logs.length} registros. Valores detalhados no histórico.`}
    >
      <path d="M30 35H670M30 95H670M30 155H670" stroke="#E2E8DF" />
      <text x="6" y="40">
        {max}
      </text>
      <text x="10" y="160">
        0
      </text>
      <polyline
        points={coordinates}
        fill="none"
        stroke="#7CA289"
        strokeWidth="3"
      />
      {coordinates.split(" ").map((p, i) => (
        <circle
          key={logs[i].id}
          cx={p.split(",")[0]}
          cy={p.split(",")[1]}
          r="4"
          fill="#2E4034"
        />
      ))}
      <text x="30" y="190">
        {date(logs[0].occurred_at)}
      </text>
      <text x="670" y="190" textAnchor="end">
        {date(logs.at(-1)!.occurred_at)}
      </text>
    </svg>
  );
}
function TreatmentDetail({ id, back }: { id: string; back: () => void }) {
  const detail = useData<{
    treatment: Treatment;
    draft: Plan;
    plan: { content: Plan; version: number } | null;
    versions: { version: number; published_at: string; content: Plan }[];
    points: Point[];
  }>(`/treatments/${id}`);
  const report = useData<Report>(`/reports/${id}`),
    sessions = useData<{
      items: { id: string; occurred_at: string; notes: string }[];
    }>(`/treatments/${id}/sessions`);
  const [editing, setEditing] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [confirmFinish, setConfirmFinish] = useState(false),
    [notes, setNotes] = useState(""),
    [at, setAt] = useState(""),
    [symptom, setSymptom] = useState(""),
    [sessionFilter, setSessionFilter] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState("");
  const reload = () => {
    detail.reload();
    report.reload();
    sessions.reload();
  };
  async function action(path: string, body: unknown) {
    setBusy(true);
    setError("");
    try {
      await api(path, "POST", body);
      reload();
      setConfirmFinish(false);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  if (detail.loading) return <Loading />;
  if (detail.error || !detail.data)
    return (
      <Notice error>
        {detail.error}
        <button onClick={back}>Voltar</button>
      </Notice>
    );
  const { treatment: t, draft, plan, versions, points } = detail.data;
  if (editing)
    return (
      <PlanEditor
        initial={draft}
        treatment={t}
        done={() => {
          setEditing(false);
          reload();
        }}
        cancel={() => setEditing(false)}
      />
    );
  const logs = report.data?.symptomLogs ?? [],
    corrected = new Set(logs.map((l) => l.correction_of)),
    symptoms = [...new Map(logs.map((l) => [l.symptom_id, l.name])).entries()];
  const chosen = symptom || symptoms[0]?.[0];
  const orderedSessions = [...(sessions.data?.items ?? [])].sort(
    (a, b) =>
      new Date(a.occurred_at).getTime() - new Date(b.occurred_at).getTime(),
  );
  const intervalStart = orderedSessions.find(
    (s) => s.id === sessionFilter,
  )?.occurred_at;
  const intervalEnd = intervalStart
    ? orderedSessions.find(
        (s) => new Date(s.occurred_at) > new Date(intervalStart),
      )?.occurred_at
    : undefined;
  const visible = logs.filter(
    (l) =>
      l.symptom_id === chosen &&
      (!intervalStart || new Date(l.occurred_at) >= new Date(intervalStart)) &&
      (!intervalEnd || new Date(l.occurred_at) < new Date(intervalEnd)) &&
      !corrected.has(l.id) &&
      (!from || l.occurred_at.slice(0, 10) >= from) &&
      (!to || l.occurred_at.slice(0, 10) <= to),
  );
  return (
    <>
      <button className="link" onClick={back}>
        ← Voltar
      </button>
      <div className="page-heading">
        <div>
          <span className="eyebrow">ACOMPANHAMENTO INDIVIDUAL</span>
          <h1>{t.title}</h1>
          <p>
            {report.data?.patient} · {t.starts_on} a {t.ends_on} · {t.timezone}
          </p>
        </div>
        <span className={`badge ${t.status}`}>{status(t.status)}</span>
      </div>
      {error && <Notice error>{error}</Notice>}
      <div className="actions">
        {t.status !== "finished" && (
          <>
            <button className="secondary" onClick={() => setEditing(true)}>
              Editar rascunho
            </button>
            <button
              disabled={busy}
              onClick={() =>
                void action(`/treatments/${id}/publish`, { version: t.version })
              }
            >
              Publicar plano revisado
            </button>
          </>
        )}
        <a
          className="button secondary"
          href={`/api/v1/reports/${id}?format=pdf`}
        >
          Exportar PDF
        </a>
      </div>
      {t.status !== "finished" && (
        <section className="panel">
          <h2>Revisão antes de publicar</h2>
          <p>
            <strong>{draft.title}</strong> · {draft.startsOn} a {draft.endsOn}
          </p>
          <p>
            Horários: {draft.times.join(" · ")} ({draft.timezone})
          </p>
          <p>
            Sintomas:{" "}
            {draft.symptoms
              .map((s) => `${s.name} (${s.min}–${s.max})`)
              .join(", ")}
          </p>
          <ul>
            {draft.instructions.map((text, i) => (
              <li key={i}>{text}</li>
            ))}
          </ul>
          <p>
            Retorno:{" "}
            {draft.followUpAt ? date(draft.followUpAt) : "Não definido"}
          </p>
          <p className="muted">
            Publicar compartilha estas orientações com o paciente e preserva as
            versões anteriores.
          </p>
        </section>
      )}
      <div className="columns">
        <section className="panel">
          <h2>Plano publicado {plan ? `· versão ${plan.version}` : ""}</h2>
          {plan ? (
            <>
              <ul>
                {plan.content.instructions.map((text, i) => (
                  <li key={i}>{text}</li>
                ))}
              </ul>
              <p>{plan.content.times.join(" · ")}</p>
              {points.map((p) => (
                <p key={p.id}>
                  {p.label} · Fonte: {p.source}
                </p>
              ))}
            </>
          ) : (
            <p>O paciente ainda não tem acesso a este rascunho.</p>
          )}
          <details>
            <summary>Histórico de versões ({versions.length})</summary>
            {versions.map((v) => (
              <article key={v.version}>
                <h3>
                  Versão {v.version} · {date(v.published_at)}
                </h3>
                {v.content.instructions.map((x, i) => (
                  <p key={i}>{x}</p>
                ))}
              </article>
            ))}
          </details>
        </section>
        <section className="panel">
          <h2>Estimulações previstas e registradas</h2>
          <strong className="big-number">
            {report.data?.adherence.percentage === null
              ? "—"
              : `${report.data?.adherence.percentage ?? "—"}%`}
          </strong>
          <p>
            {report.data?.adherence.recorded ?? 0} registradas de{" "}
            {report.data?.adherence.expected ?? 0} ocorrências já previstas.
          </p>
          <small>
            Indicador calculado por ocorrência, sem duplicidades. Não mede
            eficácia clínica.
          </small>
        </section>
      </div>
      <section className="panel">
        <h2>Evolução por sintoma</h2>
        <div className="form-grid">
          <label>
            Sintoma
            <select
              value={chosen ?? ""}
              onChange={(e) => setSymptom(e.target.value)}
            >
              <option value="">Selecione</option>
              {symptoms.map(([id, name]) => (
                <option key={id} value={id}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Desde
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </label>
          <label>
            Até
            <input
              type="date"
              min={from}
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </label>
        </div>
        <label>
          Intervalo de sessão
          <select
            value={sessionFilter}
            onChange={(e) => setSessionFilter(e.target.value)}
          >
            <option value="">Todas as sessões</option>
            {orderedSessions.map((s) => (
              <option key={s.id} value={s.id}>
                {date(s.occurred_at)}
              </option>
            ))}
          </select>
        </label>
        <Chart logs={visible} />
        <h3>Comparação descritiva entre sessões</h3>
        <p className="muted">
          Registros no intervalo entre uma sessão e a seguinte. Médias são dados
          calculados, sem interpretação clínica.
        </p>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Desde a sessão</th>
                <th>Registros</th>
                <th>Primeiro</th>
                <th>Último</th>
                <th>Média informada</th>
              </tr>
            </thead>
            <tbody>
              {orderedSessions.map((session, i) => {
                const next = orderedSessions[i + 1];
                const values = logs.filter(
                  (l) =>
                    l.symptom_id === chosen &&
                    !corrected.has(l.id) &&
                    new Date(l.occurred_at) >= new Date(session.occurred_at) &&
                    (!next ||
                      new Date(l.occurred_at) < new Date(next.occurred_at)),
                );
                return (
                  <tr key={session.id}>
                    <td>{date(session.occurred_at)}</td>
                    <td>{values.length}</td>
                    <td>{values[0]?.level ?? "—"}</td>
                    <td>{values.at(-1)?.level ?? "—"}</td>
                    <td>
                      {values.length
                        ? (
                            values.reduce((n, l) => n + l.level, 0) /
                            values.length
                          ).toFixed(1)
                        : "Dados insuficientes"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <p className="muted">
          Valores informados pelo paciente. Correções substituem os valores no
          gráfico e preservam o histórico abaixo.
        </p>
        {logs.length ? (
          <details>
            <summary>Histórico completo ({logs.length} registros)</summary>
            {logs.map((l) => (
              <div className="item" key={l.id}>
                <div>
                  <strong>
                    {l.name}: {l.level}
                  </strong>
                  <small>
                    {date(l.occurred_at)} {l.correction_of ? "· Correção" : ""}{" "}
                    {corrected.has(l.id) ? "· Substituído" : ""}
                  </small>
                  <p>{l.note}</p>
                </div>
              </div>
            ))}
          </details>
        ) : (
          <p>Nenhum registro neste acompanhamento.</p>
        )}
        {report.error && <Notice error>{report.error}</Notice>}
      </section>
      <section className="panel">
        <h2>Sessões · notas do profissional</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void action(`/treatments/${id}/sessions`, {
              occurredAt: new Date(at).toISOString(),
              notes,
            });
          }}
        >
          <label>
            Data e horário da sessão
            <input
              required
              type="datetime-local"
              value={at}
              onChange={(e) => setAt(e.target.value)}
            />
          </label>
          <label>
            Observações profissionais
            <textarea
              maxLength={5000}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </label>
          <button disabled={busy}>Registrar sessão</button>
        </form>
        {sessions.data?.items.map((s) => (
          <article className="item" key={s.id}>
            <div>
              <strong>{date(s.occurred_at)}</strong>
              <p>{s.notes}</p>
            </div>
          </article>
        ))}
        {sessions.error && <Notice error>{sessions.error}</Notice>}
      </section>
      {t.status === "active" && (
        <section className="panel">
          <h2>Encerrar acompanhamento</h2>
          <p>
            O encerramento cancela ocorrências futuras e preserva o histórico.
          </p>
          {confirmFinish ? (
            <>
              <button
                disabled={busy}
                className="danger"
                onClick={() => void action(`/treatments/${id}/finish`, {})}
              >
                Confirmar encerramento
              </button>
              <button
                className="secondary"
                onClick={() => setConfirmFinish(false)}
              >
                Cancelar
              </button>
            </>
          ) : (
            <button
              className="secondary"
              onClick={() => setConfirmFinish(true)}
            >
              Encerrar tratamento
            </button>
          )}
        </section>
      )}
    </>
  );
}
function Points() {
  const list = useData<{ items: Point[] }>("/points"),
    [label, setLabel] = useState(""),
    [source, setSource] = useState(""),
    [x, setX] = useState(""),
    [y, setY] = useState(""),
    [reviewed, setReviewed] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            CONTEÚDO SOB RESPONSABILIDADE PROFISSIONAL
          </span>
          <h1>Biblioteca de pontos</h1>
          <p>Anotações revisáveis, com fonte identificada.</p>
        </div>
      </div>
      <Notice>
        Não há uma base clínica validada incluída. As coordenadas são anotações
        relativas ao diagrama do aplicativo, e exigem revisão profissional antes
        de uso.
      </Notice>
      <div className="columns">
        <section className="panel">
          <h2>Pontos cadastrados</h2>
          {list.loading ? (
            <Loading />
          ) : list.data?.items.length ? (
            list.data.items.map((p) => (
              <article className="item" key={p.id}>
                <div>
                  <h3>{p.label}</h3>
                  <p>{p.source}</p>
                  <small>
                    Posição relativa: {p.x}, {p.y}
                  </small>
                </div>
              </article>
            ))
          ) : (
            <Empty title="Biblioteca vazia">
              Cadastre apenas conteúdo que você revisou.
            </Empty>
          )}
        </section>
        <section className="panel">
          <h2>Adicionar anotação revisada</h2>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                await api("/points", "POST", {
                  label,
                  source,
                  x: Number(x),
                  y: Number(y),
                  reviewed,
                });
                setLabel("");
                setSource("");
                setX("");
                setY("");
                setReviewed(false);
                list.reload();
              } catch (e) {
                setError(explain(e));
              } finally {
                setBusy(false);
              }
            }}
          >
            <label>
              Identificação
              <input
                required
                maxLength={100}
                value={label}
                onChange={(e) => setLabel(e.target.value)}
              />
            </label>
            <label>
              Fonte e referência de revisão
              <textarea
                required
                maxLength={500}
                value={source}
                onChange={(e) => setSource(e.target.value)}
              />
            </label>
            <div className="form-grid">
              <label>
                Coordenada X (0–1)
                <input
                  required
                  type="number"
                  step="0.001"
                  min="0"
                  max="1"
                  value={x}
                  onChange={(e) => setX(e.target.value)}
                />
              </label>
              <label>
                Coordenada Y (0–1)
                <input
                  required
                  type="number"
                  step="0.001"
                  min="0"
                  max="1"
                  value={y}
                  onChange={(e) => setY(e.target.value)}
                />
              </label>
            </div>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={reviewed}
                onChange={(e) => setReviewed(e.target.checked)}
              />
              Revisei a identificação, a fonte e as coordenadas.
            </label>
            <button disabled={!reviewed || busy}>Salvar ponto revisado</button>
            {error && <Notice error>{error}</Notice>}
          </form>
        </section>
      </div>
    </>
  );
}
function Sharing() {
  const [token, setToken] = useState(""),
    [consent, setConsent] = useState(false),
    [error, setError] = useState("");
  const links = useData<{
    items: {
      id: string;
      display_name: string;
      status: string;
      verification_status: string;
    }[];
  }>("/patient-links");
  return (
    <>
      <h1>Seu compartilhamento</h1>
      <section className="panel">
        <h2>Aceitar convite profissional</h2>
        <p>
          Ao aceitar, este profissional poderá acessar seus registros dos
          tratamentos que ele acompanha. Você pode revogar o vínculo nesta
          página.
        </p>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setError("");
            try {
              await api("/patient-links/accept", "POST", {
                token,
                consent,
                policyVersion: "2026-11-03",
              });
              setToken("");
              setConsent(false);
              links.reload();
            } catch (e) {
              setError(explain(e));
            }
          }}
        >
          <label>
            Código do convite
            <input
              required
              minLength={40}
              maxLength={100}
              value={token}
              onChange={(e) => setToken(e.target.value)}
            />
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
            />
            Autorizo o compartilhamento descrito acima (versão 2026-11-03).
          </label>
          <button disabled={!consent}>Aceitar convite</button>
          {error && <Notice error>{error}</Notice>}
        </form>
      </section>
      {links.data?.items.map((l) => (
        <div key={l.id}>
          <h2>
            {l.display_name} ·{" "}
            {l.verification_status === "verified"
              ? "Cadastro verificado"
              : "Cadastro não verificado"}
          </h2>
          {l.status === "active" ? (
            <Revoke linkId={l.id} done={links.reload} />
          ) : (
            <p>Vínculo revogado</p>
          )}
        </div>
      ))}
      <a href="/api/v1/export" className="button secondary">
        Exportar meus dados (JSON)
      </a>
    </>
  );
}
