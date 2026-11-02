-- ACT Squad · dados de demonstração
-- 2 terapeutas, 4 pacientes cada (o primeiro de cada um é o paciente padrão)
-- Senha de ambos: senha12345
-- Conecte no database act e execute este script.

-- Limpa demo anterior (ids fixos abaixo; FKs com ON DELETE CASCADE removem o restante)
DELETE FROM users WHERE id IN (
  'a1111111-1111-1111-1111-111111111111'::uuid,
  'a2222222-2222-2222-2222-222222222222'::uuid
);

INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at)
VALUES
  (
    'a1111111-1111-1111-1111-111111111111',
    'priscila@asa.test',
    '$2a$10$Xhc/pfmlNyM7qn0awZmNm.Cl4X3J9OXJ/Zqm/7DWYr5yen38HdxNq',
    'Priscila Demo',
    'therapist',
    now(),
    now()
  ),
  (
    'a2222222-2222-2222-2222-222222222222',
    'marina@asa.test',
    '$2a$10$Xhc/pfmlNyM7qn0awZmNm.Cl4X3J9OXJ/Zqm/7DWYr5yen38HdxNq',
    'Marina Demo',
    'therapist',
    now(),
    now()
  );

INSERT INTO subscriptions (id, user_id, plan, status, starts_at, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'individual', 'active', now(), now(), now()),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'individual', 'active', now(), now(), now());

INSERT INTO terms_acceptances (id, user_id, terms_version, accepted_at)
VALUES
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', '2026-10', now()),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', '2026-10', now());

-- Pacientes da Priscila (created_at escalonado: o mais antigo é o padrão)
INSERT INTO patients (id, user_id, display_name, note, is_default, created_at, updated_at) VALUES
  ('b1111111-0001-4000-8000-000000000001', 'a1111111-1111-1111-1111-111111111111', 'Paciente padrão', NULL, true,  now() - interval '4 minutes', now()),
  ('b1111111-0002-4000-8000-000000000002', 'a1111111-1111-1111-1111-111111111111', 'João (terça)', 'adolescente', false, now() - interval '3 minutes', now()),
  ('b1111111-0003-4000-8000-000000000003', 'a1111111-1111-1111-1111-111111111111', 'Ana C.', 'adulta', false, now() - interval '2 minutes', now()),
  ('b1111111-0004-4000-8000-000000000004', 'a1111111-1111-1111-1111-111111111111', 'L. S.', 'família', false, now() - interval '1 minutes', now());

-- Pacientes da Marina
INSERT INTO patients (id, user_id, display_name, note, is_default, created_at, updated_at) VALUES
  ('b2222222-0001-4000-8000-000000000001', 'a2222222-2222-2222-2222-222222222222', 'Paciente padrão', NULL, true,  now() - interval '4 minutes', now()),
  ('b2222222-0002-4000-8000-000000000002', 'a2222222-2222-2222-2222-222222222222', 'Pedro N.', 'adulto', false, now() - interval '3 minutes', now()),
  ('b2222222-0003-4000-8000-000000000003', 'a2222222-2222-2222-2222-222222222222', 'Bia', 'criança', false, now() - interval '2 minutes', now()),
  ('b2222222-0004-4000-8000-000000000004', 'a2222222-2222-2222-2222-222222222222', 'Carla M.', 'ACT', false, now() - interval '1 minutes', now());

INSERT INTO patient_states (patient_id, state, updated_at)
SELECT id, '{}'::jsonb, now()
FROM patients
WHERE user_id IN (
  'a1111111-1111-1111-1111-111111111111',
  'a2222222-2222-2222-2222-222222222222'
);
