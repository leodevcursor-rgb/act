-- ACT Squad · schema completo (PostgreSQL) — idempotente
-- 1) Conecte no database "postgres" e rode só o CREATE DATABASE (se ainda não existir).
-- 2) Conecte no database "act" e rode o restante.

-- CREATE DATABASE act WITH ENCODING = 'UTF8' TEMPLATE = template0;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$ BEGIN
  CREATE TYPE user_role AS ENUM ('therapist', 'admin');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE subscription_status AS ENUM ('active', 'expired', 'canceled');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           TEXT NOT NULL,
  password_hash   TEXT,
  name            TEXT,
  role            user_role NOT NULL DEFAULT 'therapist',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS subscriptions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  plan            TEXT NOT NULL DEFAULT 'individual',
  status          subscription_status NOT NULL DEFAULT 'active',
  starts_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  ends_at         TIMESTAMPTZ,
  provider_ref    TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions (user_id, status);

CREATE TABLE IF NOT EXISTS terms_acceptances (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  terms_version   TEXT NOT NULL,
  accepted_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT terms_user_version_unique UNIQUE (user_id, terms_version)
);

CREATE TABLE IF NOT EXISTS patients (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  display_name    TEXT NOT NULL,
  note            TEXT,
  is_default      BOOLEAN NOT NULL DEFAULT false,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  archived_at     TIMESTAMPTZ,
  CONSTRAINT patients_display_name_len CHECK (char_length(display_name) BETWEEN 1 AND 60),
  CONSTRAINT patients_note_len CHECK (note IS NULL OR char_length(note) <= 120)
);

CREATE INDEX IF NOT EXISTS idx_patients_user ON patients (user_id);
CREATE INDEX IF NOT EXISTS idx_patients_user_updated ON patients (user_id, updated_at DESC)
  WHERE archived_at IS NULL;

-- No máximo um paciente padrão (não arquivado) por usuário
CREATE UNIQUE INDEX IF NOT EXISTS uq_patients_user_default ON patients (user_id)
  WHERE is_default AND archived_at IS NULL;

CREATE TABLE IF NOT EXISTS patient_states (
  patient_id      UUID PRIMARY KEY REFERENCES patients(id) ON DELETE CASCADE,
  state           JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated ON users;
CREATE TRIGGER trg_users_updated
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_subscriptions_updated ON subscriptions;
CREATE TRIGGER trg_subscriptions_updated
  BEFORE UPDATE ON subscriptions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_patients_updated ON patients;
CREATE TRIGGER trg_patients_updated
  BEFORE UPDATE ON patients
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_patient_states_updated ON patient_states;
CREATE TRIGGER trg_patient_states_updated
  BEFORE UPDATE ON patient_states
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
