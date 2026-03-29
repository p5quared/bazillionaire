package net.peterv.bazillionaire.game.domain.powerup;

import java.util.Comparator;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public final class PlayerRankingStrategy implements TargetingStrategy {
  private final Comparator<Long> comparator;

  private PlayerRankingStrategy(Comparator<Long> comparator) {
    this.comparator = comparator;
  }

  public static PlayerRankingStrategy leading() {
    return new PlayerRankingStrategy(Comparator.reverseOrder());
  }

  public static PlayerRankingStrategy trailing() {
    return new PlayerRankingStrategy(Comparator.naturalOrder());
  }

  @Override
  public PlayerId selectTarget(GameContext context) {
    PlayerId selected = null;
    long selectedNetWorth = 0;

    for (var entry : context.players().entrySet()) {
      long netWorth = netWorth(entry.getValue(), context.currentPrices());
      if (selected == null || comparator.compare(netWorth, selectedNetWorth) < 0) {
        selectedNetWorth = netWorth;
        selected = entry.getKey();
      }
    }

    return selected;
  }

  private static long netWorth(
      GameEvent.PlayerPortfolio portfolio, Map<Symbol, Money> currentPrices) {
    long holdingsValue = 0;
    for (var holding : portfolio.holdings().entrySet()) {
      Money price = currentPrices.get(holding.getKey());
      if (price != null) {
        holdingsValue += (long) price.cents() * holding.getValue();
      }
    }
    return portfolio.cashBalance().cents() + holdingsValue;
  }
}
