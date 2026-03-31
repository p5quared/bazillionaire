package net.peterv.bazillionaire.services.stats;

import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public final class PortfolioValueCalculator {

  private PortfolioValueCalculator() {}

  public static long calculatePortfolioValueCents(
      GameEvent.PlayerPortfolio portfolio, Map<Symbol, Money> finalPrices) {
    long value = portfolio.cashBalance().cents();
    for (var entry : portfolio.holdings().entrySet()) {
      Money price = finalPrices.getOrDefault(entry.getKey(), new Money(0));
      value += (long) entry.getValue() * price.cents();
    }
    return value;
  }
}
