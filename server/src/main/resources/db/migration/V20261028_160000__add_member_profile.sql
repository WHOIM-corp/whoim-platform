ALTER TABLE member
  ADD COLUMN employee_number TEXT,
  ADD COLUMN department TEXT,
  ADD COLUMN job_title TEXT,
  ADD CONSTRAINT member_employee_number_key UNIQUE (employee_number),
  ADD CONSTRAINT member_profile_check CHECK (
    (role = 'ADMIN' AND employee_number IS NULL AND department IS NULL AND job_title IS NULL)
    OR (role = 'USER' AND employee_number IS NOT NULL AND department IS NOT NULL AND job_title IS NOT NULL)
  );
