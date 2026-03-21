package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

public interface RegimeFactory {
  RegimeStrategy nextRegime(Money lastPrice);

  RegimeStrategy nextRegime(Money lastPrice, MarketSentiment forcedSentiment);
}
