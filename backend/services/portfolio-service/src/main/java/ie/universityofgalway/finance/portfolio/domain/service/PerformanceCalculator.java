package ie.universityofgalway.finance.portfolio.domain.service;

import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PerformanceCalculator {

    private static final int AVG_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PerformanceCalculator() {}

    /** Shares still held after all trades. Lets the service decide whether a price is needed. */
    public static BigDecimal netQuantity(List<Trade> tradesOldestFirst) {
        BigDecimal qty = BigDecimal.ZERO;
        for (Trade t : tradesOldestFirst) {
            qty = t.getType() == TradeType.BUY ? qty.add(t.getQuantity()) : qty.subtract(t.getQuantity());
        }
        return qty;
    }

    /**
     * @param tradesOldestFirst all trades for ONE ticker, oldest first
     * @param currentPrice      may be null only if the position is closed
     */
    public static HoldingPerformance calculate(String ticker,
                                               List<Trade> tradesOldestFirst,
                                               BigDecimal currentPrice) {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal avg = BigDecimal.ZERO.setScale(AVG_SCALE);
        BigDecimal realized = BigDecimal.ZERO;

        for (Trade t : tradesOldestFirst) {
            if (t.getType() == TradeType.BUY) {
                BigDecimal newQty = qty.add(t.getQuantity());
                BigDecimal totalCost = qty.multiply(avg).add(t.getQuantity().multiply(t.getPrice()));
                avg = totalCost.divide(newQty, AVG_SCALE, ROUNDING);
                qty = newQty;
            } else {
                if (t.getQuantity().compareTo(qty) > 0) {
                    throw new IllegalStateException(
                            "Trade history for " + ticker + " sells more shares than were held");
                }
                // Profit on this sale uses the average cost at this moment in history.
                realized = realized.add(t.getPrice().subtract(avg).multiply(t.getQuantity()));
                qty = qty.subtract(t.getQuantity());   // average is unchanged by a sale
            }
        }

        BigDecimal realizedRounded = money(realized);

        // Closed position: nothing left to value, so no price is needed.
        if (qty.signum() == 0) {
            BigDecimal zero = money(BigDecimal.ZERO);
            return new HoldingPerformance(ticker, qty, avg, null,
                    zero, zero, zero, zero, realizedRounded);
        }

        if (currentPrice == null) {
            throw new IllegalArgumentException("Current price required for open position " + ticker);
        }

        BigDecimal costBasis = money(avg.multiply(qty));
        BigDecimal marketValue = money(currentPrice.multiply(qty));
        BigDecimal unrealized = money(currentPrice.subtract(avg).multiply(qty));
        BigDecimal percent = costBasis.signum() == 0
                ? money(BigDecimal.ZERO)
                : unrealized.multiply(HUNDRED).divide(costBasis, MONEY_SCALE, ROUNDING);

        return new HoldingPerformance(ticker, qty, avg, currentPrice,
                costBasis, marketValue, unrealized, percent, realizedRounded);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }
}
