package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;

import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Optional;

/**
 * Normalises caller-supplied tickers before requesting quotes from a provider.
 */
@Service
public class QuoteService {

    private final MarketDataProvider marketDataProvider;

    /**
     * Creates the application service.
     *
     * @param marketDataProvider provider selected by the active Spring configuration
     */
    public QuoteService(MarketDataProvider marketDataProvider){
        this.marketDataProvider = marketDataProvider;
    }

    /**
     * Trims the ticker and uppercases it using {@link Locale#ROOT} before lookup.
     *
     * @param rawSymbol caller-supplied ticker; may be null or blank
     * @return the quote, or an empty optional for null, blank or symbols with no quote data
     * @throws ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException if the
     *     configured provider fails without a usable fallback
     */
    public Optional<Quote> getQuote(String rawSymbol) {

        if (rawSymbol == null){
            return Optional.empty();
        }

        String trimmed = rawSymbol.trim();

        //if only spaces typed
        if (trimmed.isEmpty()){
            return Optional.empty();
        }

        //uppercase with standard English rules: "aapl" becomes "AAPL"
        //Locale.ROOT is Java's language neutral local
        // - ensures uppercasing behaves consistently regardless of the computer's regional settings
        String symbol = trimmed.toUpperCase(Locale.ROOT);

        //ask the provider for the price
        return marketDataProvider.findQuote(symbol);
    }


}
