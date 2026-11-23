package ie.universityofgalway.finance.portfolio.domain.model;

/**
 * Whether a {@link Trade} added shares to a holding or removed them.
 * Stored by name in the database, so renaming a constant would break existing rows.
 */
public enum TradeType {
    /** Shares were bought (or an earlier purchase was recorded). */
    BUY,
    /** Shares were sold. */
    SELL
}
