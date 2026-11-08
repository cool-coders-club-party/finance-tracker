ALTER TABLE transaction.transactions
    ALTER COLUMN user_id TYPE UUID USING NULL::uuid;