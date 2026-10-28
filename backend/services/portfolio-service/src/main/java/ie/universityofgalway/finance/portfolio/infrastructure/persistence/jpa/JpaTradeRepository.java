package ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaTradeRepository extends JpaRepository<TradeEntity, Long> {

    @Query("""
            select t from TradeEntity t
            join fetch t.holding h
            where h.portfolio.id = :portfolioId
            order by t.executedAt asc, t.id asc
            """)
    List<TradeEntity> findAllByPortfolioId(@Param("portfolioId") Long portfolioId);
}