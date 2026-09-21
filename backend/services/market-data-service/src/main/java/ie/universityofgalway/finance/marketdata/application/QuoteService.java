package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;

import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Optional;

//Tells Spring to create one QuoteService when the app starts
@Service
public class QuoteService {

    //where prices come from (the stub, for now)
    private final MarketDataProvider marketDataProvider;

    //Spring passes in the provider automatically
    public QuoteService(MarketDataProvider marketDataProvider){
        this.marketDataProvider = marketDataProvider;
    }

    //Looks up a quote, Returns empty if the symbol is blank or unknown
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
