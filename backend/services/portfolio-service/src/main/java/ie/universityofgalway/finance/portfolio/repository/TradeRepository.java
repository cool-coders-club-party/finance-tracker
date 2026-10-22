package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("""
            select t from Trade t
            join fetch t.holding h
            where h.portfolio.id = :portfolioId
            order by t.executedAt asc, t.id asc
            """)
    List<Trade> findAllByPortfolioId(@Param("portfolioId") Long portfolioId);
}
