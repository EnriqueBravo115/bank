-- The card security code is stored again (V6 dropped it): it is generated when the card is issued.
-- Cards issued in between get a new random one.
ALTER TABLE card ADD COLUMN cvv VARCHAR(3);
UPDATE card SET cvv = LPAD(FLOOR(RANDOM() * 1000)::INT::TEXT, 3, '0');
ALTER TABLE card ALTER COLUMN cvv SET NOT NULL;
