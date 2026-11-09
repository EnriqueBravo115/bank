-- The cvv is stored with the purchase (V3 dropped it) so a PENDING purchase can be retried with it.
ALTER TABLE purchase_transaction ADD COLUMN cvv VARCHAR(4);
