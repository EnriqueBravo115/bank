INSERT INTO transactions (id, source_identifier, identifier_type, transaction_code, amount, description, reason, transaction_date, currency, transaction_type, transaction_status)
VALUES (1, '646180110400000001', 'CLABE', 'TXN-2024-001', 1500.00, 'Pago de renta enero', 'Transferencia mensual de renta', '2024-01-15 09:30:00', 'MX', 'TRANSFER', 'COMPLETED');

INSERT INTO transfer_transaction (id, target_identifier, target_identifier_type)
VALUES (1, '002180700148685530', 'CLABE');

INSERT INTO transactions (id, source_identifier, identifier_type, transaction_code, amount, description, reason, transaction_date, currency, transaction_type, transaction_status)
VALUES (2, '4111111111111111', 'CARD', 'TXN-2024-002', 349.99, 'Compra en Liverpool', 'Compra de ropa de temporada', '2024-01-16 14:20:00', 'MX', 'PURCHASE', 'COMPLETED');

INSERT INTO purchase_transaction (id, source_card_number, cvv, merchant_code, merchant_category, pos_id)
VALUES (2, '4111111111111111', '123', 'MERC-LIV-001', 'RETAIL_CLOTHING', 'POS-00234');

INSERT INTO transactions (id, source_identifier, identifier_type, transaction_code, amount, description, reason, transaction_date, currency, transaction_type, transaction_status)
VALUES (3, '646180110400000003', 'CLABE', 'TXN-2024-003', 875.50, 'Pago de CFE febrero', 'Pago de luz bimestral', '2024-01-17 11:00:00', 'MX', 'SERVICE', 'PROCESSING');

INSERT INTO service_transaction (id, payment_reference, service_type)
VALUES (3, 'CFE-RPT-20240117-0099', 'ELECTRICITY');

INSERT INTO transactions (id, source_identifier, identifier_type, transaction_code, amount, description, reason, transaction_date, currency, transaction_type, transaction_status)
VALUES (4, '4532015112830366', 'CARD', 'TXN-2024-004', 2000.00, 'Retiro en cajero Banamex', 'Efectivo para gastos personales', '2024-01-18 16:45:00', 'MX', 'WITHDRAWAL', 'COMPLETED');

INSERT INTO withdrawal_transaction (id, atm_location, atm_session_id, receipt_number, branch_code, teller_id, transaction_fee, security_verification_method, withdrawal_method)
VALUES (4, 'Av. Insurgentes Sur 1234, CDMX', 'ATM-SESSION-20240118-4521', 'RCP-20240118-0056', 'BNX-SUC-007', NULL, 18.00, 'PIN', 'DEBIT_CARD');

SELECT setval('transactions_id_seq', (SELECT MAX(id) FROM transactions));
