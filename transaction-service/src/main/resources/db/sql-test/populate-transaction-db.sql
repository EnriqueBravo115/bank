INSERT INTO users (id)
VALUES
(1),
(2),
(3);

INSERT INTO account (id, balance, account_number, currency, user_id)
VALUES
(1, 1500.00, 'US123456789011111111', 'US', 1),
(2, 5000.00, 'US098765432122222222', 'US', 1),
(3, 750.50, 'CA456789012333333333', 'US', 2);

INSERT INTO transaction (id, amount, description, transaction_date, transaction_type, transaction_status, source_account_id, target_account_id, original_transaction_id)
VALUES
(1, 100.00, 'Test transaction 1', '2021-01-15 09:30:45', 'TRANSFER', 'COMPLETED', 1, NULL, NULL),
(2, 50.00, 'Test transaction 2', '2022-01-16 14:15:22', 'TRANSFER', 'COMPLETED', 1, 3, NULL),
(3, 200.00, 'Test transaction 3', '2023-01-31 08:00:00', 'TRANSFER', 'COMPLETED', NULL, 1, NULL);
