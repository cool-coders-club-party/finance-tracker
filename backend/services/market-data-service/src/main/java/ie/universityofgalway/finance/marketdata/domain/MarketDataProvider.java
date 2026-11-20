package ie.universityofgalway.finance.marketdata.domain;

import java.util.Optional;

/**
 * Port for retrieving quotes independently of the external provider or caching mechanism.
 */
public interface MarketDataProvider {
    /**
     * Finds a quote for a normalised ticker.
     *
     * <p>Absence represents missing quote data. Transport failures must remain failures
     * so a caching decorator can distinguish them from an unknown symbol.</p>
     *
     * @param symbol non-null ticker, trimmed and uppercased by the caller
     * @return a quote, or an empty optional if the symbol has no usable quote
     * @throws org.springframework.web.client.RestClientException if a live HTTP provider request fails
     * @throws MarketDataUnavailableException if a decorated provider cannot supply a quote or cached fallback
     */
    Optional<Quote> findQuote(String symbol);
}
