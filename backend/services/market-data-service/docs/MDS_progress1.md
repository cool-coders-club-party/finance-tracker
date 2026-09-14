
## Explaining: `application.yml`
`market-data-service/src/main/resources/application.yml` : The settings file for my service. Spring boot reads it automatically when the app starts, and it holds the choices I don't want written in the Java code.

```yaml
spring:
  application:
    name: market-data-service
  datasource:
    url: jdbc:h2:mem:marketdata;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
server:
  port: 8084
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

^^ It's a settings file. Your app reads it when it starts.

Code = what your app does. This file = the details it needs to know.

Your file says four things:
1. **My name is market-data-service** — so you can tell which app is which in the logs
2. **My database is here** — a temporary one that lives in memory and disappears when you stop the app
3. **Build my database tables for me** — so you don't have to write them by hand
4. **I listen on port 8084** — like a door number. Six apps, six different doors, or they'd clash.

Why not put this in the code? Because these things change depending on where the app is running. Easier to edit one line in a settings file than to change code and rebuild.

> I just added `profiles: active: stub`
![](../../../../../../../Library/Mobile%20Documents/iCloud~md~obsidian/Documents/Year%203/Screenshots/Screenshot%202026-09-14%20at%2010.17.50.png)
Start in stub mode: switches on fake price provider 
---

Then I created `domain`, `application`, `api`, `infrastructure` packages.
![](../../../../../../../Library/Mobile%20Documents/iCloud~md~obsidian/Documents/Year%203/Screenshots/Screenshot%202026-09-14%20at%2010.22.42.png)

---

Created `Quote.java`

I used `record` instead of `class`
```java
package ie.universityofgalway.finance.marketdata.domain;  
  
import java.math.BigDecimal;  
import java.time.Instant;  
  
//record is a short way to write a class that only holds values  
public record Quote (String symbol, BigDecimal price, Instant asOf) {}
```

`BigDecimal` for accuracy 
`Instant` is a precise moment on the universal UTC timeline
`Instant asOf` will store when the quoted price was obtained 

---

Created `MarketDataProvider.java`

```java
package ie.universityofgalway.finance.marketdata.domain;  
  
import java.util.Optional;  
  
public interface MarketDataProvider {  
    //Optional wraps the possible quote, it makes the "nothing was found" case explicit  
    //instead of returning `null`    
    Optional<Quote> findQuote(String symbol);  
}
```

---

Created `infrastructure/StubMarketDataProvider.java` 

This is Stub data for testing, to have a JSON endpoint etc.

```java
package ie.universityofgalway.finance.marketdata.infrastructure;  
  
import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;  
import ie.universityofgalway.finance.marketdata.domain.Quote;  
import org.springframework.context.annotation.Profile;  
import org.springframework.stereotype.Component;  
  
import javax.swing.text.html.Option;  
import java.math.BigDecimal;  
import java.time.Instant;  
import java.util.HashMap;  
import java.util.Map;  
import java.util.Optional;  
  
@Component  
@Profile("stub")  
public class StubMarketDataProvider implements MarketDataProvider {  
  
    private static final Map<String, BigDecimal> PRICES = new HashMap<>();  
  
    //filling the PRICES HashMap  
    static {  
        PRICES.put("AAPL", new BigDecimal("192.45"));  
        PRICES.put("MSFT", new BigDecimal("415.20"));  
        PRICES.put("TSLA", new BigDecimal("248.90"));  
    }  
  
    @Override  
    public Optional<Quote> findQuote(String symbol) {  
  
        if (symbol == null) {  
            return Optional.empty();  
        }  
  
        String key = symbol.trim().toUpperCase();  
  
        BigDecimal price = PRICES.get(key);  
  
        if (price == null) {  
            return Optional.empty();  
        }  
  
        Quote quote = new Quote(key, price, Instant.now());  
  
        return Optional.of(quote);  
    }  
  
  
}
```

---

## TESTING IT ALL WORKS WITH JUNIT
I had to create `src/test/java` for JUnit to know where the test files are.

My `pom.xml` for my micro service was missing JUnit testing dependency, so I had to add it manually into `pom.xml`
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Here is my JUnit tests `src/test/java/ie..../infrastructure/StubMarketDataProviderTest.java`
```java
package ie.universityofgalway.finance.marketdata.infrastructure;  
  
import ie.universityofgalway.finance.marketdata.domain.Quote;  
import org.junit.jupiter.api.Test;  
  
import java.math.BigDecimal;  
import java.util.Optional;  
  
import static org.junit.jupiter.api.Assertions.assertEquals;  
import static org.junit.jupiter.api.Assertions.assertTrue;  
  
class StubMarketDataProviderTest {  
  
    @Test  
    void returnsPriceForKnownSymbol() {  
  
        StubMarketDataProvider provider = new StubMarketDataProvider();  
  
        Optional<Quote> result = provider.findQuote("AAPL");  
  
        assertTrue(result.isPresent());  
  
        Quote quote = result.get();  
  
        assertEquals("AAPL", quote.symbol());  
        assertEquals(new BigDecimal("192.45"), quote.price());  
    }  
  
    @Test  
    void handlesMessyInput() {  
  
        StubMarketDataProvider provider = new StubMarketDataProvider();  
  
        Optional<Quote> result = provider.findQuote("  aapl  ");  
  
        assertTrue(result.isPresent());  
        assertEquals("AAPL", result.get().symbol());  
    }  
  
    @Test  
    void returnsNothingForUnknownSymbol() {  
  
        StubMarketDataProvider provider = new StubMarketDataProvider();  
  
        Optional<Quote> result = provider.findQuote("NOTAREALSTOCK");  
  
        assertTrue(result.isEmpty());  
    }  
  
    @Test  
    void returnsNothingForNull() {  
  
        StubMarketDataProvider provider = new StubMarketDataProvider();  
  
        Optional<Quote> result = provider.findQuote(null);  
  
        assertTrue(result.isEmpty());  
    }  
}
```

And then reload Maven so it downloads it:
```bash
./mvnw -pl services/market-data-service test
```

Success!!
![](../../../../../../../Library/Mobile%20Documents/iCloud~md~obsidian/Documents/Year%203/Screenshots/Screenshot%202026-09-14%20at%2011.51.35.png)

Message for the lads:
> The pom.xml is missing the test SpringBoot framework!!




