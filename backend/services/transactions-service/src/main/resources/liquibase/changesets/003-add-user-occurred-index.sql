
CREATE INDEX idx_transactions_user_occurred_at
    ON transactions (user_id, occurred_at);
