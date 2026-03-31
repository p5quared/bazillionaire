package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class PortfolioValueCalculatorTest {

  @Test
  void cashOnlyReturnsBalance() {
    var portfolio = new GameEvent.PlayerPortfolio(new Money(500_00), Map.of());
    long value = PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, Map.of());
    assertEquals(500_00, value);
  }

  @Test
  void holdingsOnlyReturnsSumOfHoldingsTimesPrice() {
    var portfolio =
        new GameEvent.PlayerPortfolio(
            new Money(0), Map.of(new Symbol("AAPL"), 10, new Symbol("GOOG"), 5));
    var prices =
        Map.of(
            new Symbol("AAPL"), new Money(100_00),
            new Symbol("GOOG"), new Money(200_00));

    long value = PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, prices);
    assertEquals(10 * 100_00 + 5 * 200_00, value);
  }

  @Test
  void mixedCashAndHoldings() {
    var portfolio = new GameEvent.PlayerPortfolio(new Money(300_00), Map.of(new Symbol("AAPL"), 4));
    var prices = Map.of(new Symbol("AAPL"), new Money(50_00));

    long value = PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, prices);
    assertEquals(300_00 + 4 * 50_00, value);
  }

  @Test
  void missingPriceTreatsValueAsZero() {
    var portfolio =
        new GameEvent.PlayerPortfolio(new Money(100_00), Map.of(new Symbol("MISSING"), 10));

    long value = PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, Map.of());
    assertEquals(100_00, value);
  }

  @Test
  void emptyPortfolioReturnsZero() {
    var portfolio = new GameEvent.PlayerPortfolio(new Money(0), Map.of());
    long value = PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, Map.of());
    assertEquals(0, value);
  }
}
