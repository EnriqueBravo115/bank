-- Emails are stored lowercase: lookups and the UNIQUE constraint then ignore case.
UPDATE users SET email = LOWER(email);

-- A phone number is unique together with its country code: +1 5512345678 and +52 5512345678 differ.
ALTER TABLE users DROP CONSTRAINT users_phone_number_key;
ALTER TABLE users ADD CONSTRAINT users_phone_code_number_key UNIQUE (phone_code, phone_number);

-- Not everybody has a second surname.
ALTER TABLE user_profiles ALTER COLUMN second_surname DROP NOT NULL;
