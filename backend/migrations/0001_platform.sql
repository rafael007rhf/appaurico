CREATE TABLE app_user (
 id uuid PRIMARY KEY, issuer text NOT NULL, subject text NOT NULL, role text NOT NULL CHECK(role IN ('patient','professional')),
 display_name text NOT NULL, email text, email_verified boolean NOT NULL DEFAULT false,
 revision bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(issuer,subject)
);
CREATE TABLE patient_profile (user_id uuid PRIMARY KEY REFERENCES app_user(id) ON DELETE RESTRICT);
CREATE TABLE professional_profile (user_id uuid PRIMARY KEY REFERENCES app_user(id) ON DELETE RESTRICT, verification_status text NOT NULL DEFAULT 'unverified' CHECK(verification_status IN ('unverified','verified')), registration text);
CREATE TABLE patient_invitation (id uuid PRIMARY KEY, professional_id uuid NOT NULL REFERENCES professional_profile(user_id), email text NOT NULL, token_hash text NOT NULL UNIQUE, expires_at timestamptz NOT NULL, accepted_by uuid REFERENCES patient_profile(user_id), created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE patient_professional_link (id uuid PRIMARY KEY, patient_id uuid NOT NULL REFERENCES patient_profile(user_id), professional_id uuid NOT NULL REFERENCES professional_profile(user_id), status text NOT NULL DEFAULT 'active' CHECK(status IN ('active','revoked')), version int NOT NULL DEFAULT 1, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(patient_id,professional_id));
CREATE INDEX link_professional ON patient_professional_link(professional_id,status);
CREATE TABLE consent_record (id uuid PRIMARY KEY, link_id uuid NOT NULL REFERENCES patient_professional_link(id), patient_id uuid NOT NULL REFERENCES patient_profile(user_id), policy_version text NOT NULL, action text NOT NULL CHECK(action IN ('accepted','revoked')), created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE audit_event (id uuid PRIMARY KEY, actor_id uuid NOT NULL REFERENCES app_user(id), action text NOT NULL, resource_id uuid, created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX audit_actor_time ON audit_event(actor_id,created_at);
CREATE TABLE treatment (
 id uuid PRIMARY KEY, patient_id uuid NOT NULL REFERENCES patient_profile(user_id), professional_id uuid NOT NULL REFERENCES professional_profile(user_id),
 title text NOT NULL, status text NOT NULL DEFAULT 'draft' CHECK(status IN ('draft','active','finished')), version int NOT NULL DEFAULT 1,
 starts_on date NOT NULL, ends_on date NOT NULL CHECK(ends_on >= starts_on), timezone text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), finished_at timestamptz
);
CREATE INDEX treatment_patient ON treatment(patient_id,status);
CREATE INDEX treatment_professional ON treatment(professional_id,status);
CREATE TABLE treatment_plan (id uuid PRIMARY KEY REFERENCES treatment(id), current_version int NOT NULL DEFAULT 0, draft jsonb NOT NULL);
CREATE TABLE treatment_plan_version (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), version int NOT NULL, content jsonb NOT NULL, published_at timestamptz NOT NULL DEFAULT now(), UNIQUE(treatment_id,version));
CREATE TABLE treatment_symptom (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), name text NOT NULL, min_value int NOT NULL DEFAULT 0, max_value int NOT NULL DEFAULT 10 CHECK(max_value > min_value), UNIQUE(id,treatment_id));
CREATE TABLE auricular_point (id uuid PRIMARY KEY, professional_id uuid NOT NULL REFERENCES professional_profile(user_id), label text NOT NULL, source text NOT NULL, x numeric NOT NULL CHECK(x BETWEEN 0 AND 1), y numeric NOT NULL CHECK(y BETWEEN 0 AND 1), reviewed_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE treatment_point (plan_version_id uuid NOT NULL REFERENCES treatment_plan_version(id), point_id uuid NOT NULL REFERENCES auricular_point(id), PRIMARY KEY(plan_version_id,point_id));
CREATE TABLE treatment_instruction (id uuid PRIMARY KEY, plan_version_id uuid NOT NULL REFERENCES treatment_plan_version(id), text text NOT NULL);
CREATE TABLE treatment_session (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), occurred_at timestamptz NOT NULL, notes text, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(id,treatment_id));
CREATE TABLE follow_up_appointment (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), scheduled_at timestamptz NOT NULL, status text NOT NULL DEFAULT 'scheduled' CHECK(status IN ('scheduled','cancelled','completed')), created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE scheduled_stimulation (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), plan_version_id uuid NOT NULL REFERENCES treatment_plan_version(id), scheduled_at timestamptz NOT NULL, status text NOT NULL DEFAULT 'scheduled' CHECK(status IN ('scheduled','cancelled')), UNIQUE(treatment_id,scheduled_at), UNIQUE(id,treatment_id));
CREATE INDEX stimulation_schedule ON scheduled_stimulation(treatment_id,scheduled_at);
CREATE TABLE symptom_log (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), patient_id uuid NOT NULL REFERENCES patient_profile(user_id), symptom_id uuid NOT NULL, level int NOT NULL, note text, occurred_at timestamptz NOT NULL, correction_of uuid UNIQUE REFERENCES symptom_log(id), session_id uuid, created_at timestamptz NOT NULL DEFAULT now(), FOREIGN KEY(symptom_id,treatment_id) REFERENCES treatment_symptom(id,treatment_id), FOREIGN KEY(session_id,treatment_id) REFERENCES treatment_session(id,treatment_id));
CREATE INDEX symptom_history ON symptom_log(treatment_id,occurred_at,id);
CREATE TABLE stimulation_log (id uuid PRIMARY KEY, treatment_id uuid NOT NULL REFERENCES treatment(id), patient_id uuid NOT NULL REFERENCES patient_profile(user_id), scheduled_id uuid NOT NULL UNIQUE, occurred_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), FOREIGN KEY(scheduled_id,treatment_id) REFERENCES scheduled_stimulation(id,treatment_id));
CREATE TABLE reminder_preference (patient_id uuid NOT NULL REFERENCES patient_profile(user_id), treatment_id uuid NOT NULL REFERENCES treatment(id), enabled boolean NOT NULL DEFAULT false, updated_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(patient_id,treatment_id));
CREATE TABLE device_registration (id uuid NOT NULL, user_id uuid NOT NULL REFERENCES app_user(id), created_at timestamptz NOT NULL DEFAULT now(), last_seen_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(user_id,id));
CREATE TABLE sync_operation (user_id uuid NOT NULL REFERENCES app_user(id), operation_id uuid NOT NULL, device_id uuid NOT NULL, payload_hash text NOT NULL, result jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(user_id,operation_id), FOREIGN KEY(user_id,device_id) REFERENCES device_registration(user_id,id));
CREATE TABLE sync_change (patient_id uuid NOT NULL REFERENCES patient_profile(user_id), revision bigint NOT NULL, kind text NOT NULL, resource_id uuid NOT NULL, payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(patient_id,revision));
CREATE TABLE web_session (token_hash text PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_user(id), expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE auth_flow (state_hash text PRIMARY KEY, verifier text NOT NULL, nonce text NOT NULL, expires_at timestamptz NOT NULL);
