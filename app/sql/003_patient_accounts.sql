-- ACT Squad · contas de paciente (login readonly)
-- Aplique no database act já existente (initdb só roda em volume novo).

DO $$ BEGIN
  ALTER TYPE user_role ADD VALUE 'patient';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS cpf_digits VARCHAR(11);

ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS account_user_id UUID;

DO $$ BEGIN
  ALTER TABLE patients
    ADD CONSTRAINT patients_account_user_fk
    FOREIGN KEY (account_user_id) REFERENCES users(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_patients_account_user
  ON patients (account_user_id)
  WHERE account_user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_patients_cpf_digits
  ON patients (cpf_digits)
  WHERE cpf_digits IS NOT NULL;

DO $$ BEGIN
  ALTER TABLE patients
    ADD CONSTRAINT patients_cpf_digits_len
    CHECK (cpf_digits IS NULL OR char_length(cpf_digits) = 11);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
