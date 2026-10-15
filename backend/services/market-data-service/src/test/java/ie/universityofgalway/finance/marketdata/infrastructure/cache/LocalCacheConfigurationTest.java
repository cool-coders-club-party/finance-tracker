package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = {
        "market-data.twelve-data.base-url=https://example.test",
        "market-data.twelve-data.api-key=test-key"
})
@ActiveProfiles("local")
class LocalCacheConfigurationTest {

    @Autowired
    private MarketDataProvider provider;

    @Test
    void localProfileWrapsTwelveDataProviderWithCache() {
        //Checks the local profile creates the cache without calling Twelve Data.
        assertInstanceOf(CachingMarketDataProvider.class, provider);
    }
}
