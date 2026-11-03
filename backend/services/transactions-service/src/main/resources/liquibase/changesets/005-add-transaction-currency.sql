ALTER TABLE transaction.transactions
    ADD COLUMN currency VARCHAR(3);

UPDATE transaction.transactions
SET currency = 'EUR';

ALTER TABLE transaction.transactions
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE transaction.transactions
    ADD CONSTRAINT chk_transactions_currency
        CHECK (currency IN ('EUR', 'USD', 'GBP'));