INSERT INTO users (id, username, email, full_name, password, country, gender, role, password_reset_code, country_code, activation_code, phone_code, birthday, phone_number, active, registration_date, updated_at)
VALUES
(1, 'johndoe', 'john.doe@example.com', 'John Doe', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQRqQz6W3f6U7Hl7U1q7L1b1JZzq3Gq', 'United States', 'Male', 'USER', NULL, 'US', 'ACT123', '+1', '1985-05-15', 15551234567, TRUE, '2022-01-10 09:15:22', '2022-01-10 09:15:22'),
(2, 'janedoe', 'jane.doe@example.com', 'Jane Doe', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQRqQz6W3f6U7Hl7U1q7L1b1JZzq3Gq', 'Canada', 'Female', 'USER', NULL, 'CA', 'ACT456', '+1', '1990-08-22', 15559876543, TRUE, '2022-02-15 14:30:10', '2022-02-20 11:22:33'),
(3, 'mikesmith', 'mike.smith@example.com', 'Mike Smith', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQRqQz6W3f6U7Hl7U1q7L1b1JZzq3Gq', 'United Kingdom', 'Male', 'ADMIN', NULL, 'UK', 'ACT789', '+44', '1978-11-30', 447700123456, TRUE, '2021-12-05 08:45:00', '2022-03-01 16:20:15');

INSERT INTO account (id, balance, account_number, account_type, currency, status, creation_date, user_id)
VALUES
(1, 1500.00, 'US1234567890', 'CHECKING', 'USD', 'OPEN', '2022-01-15 10:30:00', 1),
(2, 5000.00, 'US0987654321', 'SAVING', 'USD', 'OPEN', '2022-01-15 10:30:05', 1),
(3, 750.50, 'CA4567890123', 'CHECKING', 'USD', 'OPEN', '2022-02-20 14:15:22', 2);

INSERT INTO transaction (id, amount, description, transaction_date, transaction_type, transaction_status, source_account_id, target_account_id, original_transaction_id)
VALUES
(1, 100.00, 'Test transaction 1', '2021-01-15 09:30:45', 'TRANSFER', 'COMPLETED', 1, NULL, NULL),
(2, 50.00, 'Test transaction 2', '2022-01-16 14:15:22', 'TRANSFER', 'COMPLETED', 1, 3, NULL),
(3, 200.00, 'Test transaction 3', '2023-01-31 08:00:00', 'TRANSFER', 'COMPLETED', NULL, 1, NULL);
