CREATE TABLE governance_request (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_user(id),
 kind text NOT NULL CHECK(kind IN ('deletion','professional_verification','guardian_review')),
 status text NOT NULL DEFAULT 'requested' CHECK(status IN ('requested','under_review','needs_information','approved','denied','cancelled')),
 details jsonb NOT NULL, decision text, reviewer_id uuid REFERENCES app_user(id),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), version int NOT NULL DEFAULT 1
);
CREATE INDEX governance_user ON governance_request(user_id,created_at);
CREATE UNIQUE INDEX governance_open_request ON governance_request(user_id,kind) WHERE status IN ('requested','under_review','needs_information');
ALTER TABLE web_session ADD COLUMN operations_access boolean NOT NULL DEFAULT false;
