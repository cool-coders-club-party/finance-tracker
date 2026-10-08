package ie.universityofgalway.finance.portfolio.dto;

import ie.universityofgalway.finance.portfolio.model.Holding;

import java.math.BigDecimal;

public record HoldingResponse(
        Long id,
        String ticker,
        BigDecimal quantity,
        BigDecimal avgPurchasePrice
) {
    // A static factory method to build this DTO from the entity keeps the
    // mapping logic in one place, rather than scattered across every controller
    // method that needs to return a Holding.
    public static HoldingResponse from(Holding holding) {
        return new HoldingResponse(
                holding.getId(),
                holding.getTicker(),
                holding.getQuantity(),
                holding.getAvgPurchasePrice()
        );
    }
}
