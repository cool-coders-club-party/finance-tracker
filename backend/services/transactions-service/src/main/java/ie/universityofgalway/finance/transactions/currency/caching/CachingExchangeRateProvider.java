package ie.universityofgalway.finance.transactions.currency.caching;

import ie.universityofgalway.finance.transactions.currency.ExchangeRate;
import ie.universityofgalway.finance.transactions.currency.ExchangeRateProvider;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Primary
@Slf4j
public class CachingExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;

    public CachingExchangeRateProvider(
            @Qualifier("frankfurterExchangeRateProvider")
            ExchangeRateProvider delegate,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${frankfurter.cache-ttl}") Duration cacheTtl
    ) {
        if (cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }

        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = cacheTtl;
    }


    @Override
    public ExchangeRate getRate(
            TransactionCurrency baseCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date
    ) {
        if (Objects.equals(baseCurrency, targetCurrency)) return delegate.getRate(baseCurrency, targetCurrency, date);

        ExchangeRateCacheKey key =
                new ExchangeRateCacheKey(baseCurrency, targetCurrency, date);

        ExchangeRate cached = readCachedRate(key);
        if (Objects.nonNull(cached)) return cached;

        ExchangeRate fetched = delegate.getRate(baseCurrency, targetCurrency, date);
        cacheRate(key, fetched);
        return fetched;
    }

    private void cacheRate(ExchangeRateCacheKey key, ExchangeRate rate) {
        try {
            redisTemplate.opsForValue()
                    .set(
                            key.toRedisKey(),
                            objectMapper.writeValueAsString(rate),
                            cacheTtl
                    );
        } catch (DataAccessException | JacksonException exception) {
            log.warn("Could not cache exchange rate for {}", key, exception);
        }
    }

    private ExchangeRate readCachedRate(ExchangeRateCacheKey key) {
        try {
            String json = redisTemplate.opsForValue().get(key.toRedisKey());
            if (Objects.isNull(json)) return null;

            ExchangeRate rate = objectMapper.readValue(json, ExchangeRate.class);

            if (isValidCacheRate(rate, key)) return rate;
            log.warn("Ignoring invalid cached exchange rate for {}", key);
        } catch (DataAccessException | JacksonException exception) {
            log.warn("Could not read exchange-rate cache for {}; fetching from provider",
                    key, exception);
        }
        return null;

    }

    private boolean isValidCacheRate(ExchangeRate rate, ExchangeRateCacheKey key) {
        return Objects.nonNull(rate)
                && rate.baseCurrency() == key.baseCurrency()
                && rate.targetCurrency() == key.targetCurrency()
                && Objects.nonNull(rate.rateDate())
                && !rate.rateDate().isAfter(key.requestedDate())
                && Objects.nonNull(rate.rate())
                && rate.rate().signum() > 0;
    }
}
