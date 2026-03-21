package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.ticker.regime.MarketSentiment;
import net.peterv.bazillionaire.game.domain.ticker.regime.SentimentInfluence;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class SentimentBoostPowerup extends Powerup {
  private final Random random;
  private Symbol targetSymbol;

  public SentimentBoostPowerup(Random random) {
    super(0);
    this.random = random;
  }

  @Override
  public void setSymbolTarget(Symbol target) {
    this.targetSymbol = target;
  }

  @Override
  public List<PowerupEffect> onActivate() {
    if (targetSymbol == null) {
      return List.of();
    }
    int delay = 1 + random.nextInt(3);
    int duration = 2 + random.nextInt(4);
    var influence = new SentimentInfluence(MarketSentiment.BULL, delay, duration);
    return List.of(
        new PowerupEffect.InfluenceSentiment(targetSymbol, influence),
        new PowerupEffect.Emit(
            GameMessage.broadcast(new GameEvent.SentimentBoostActivated(targetSymbol))));
  }

  @Override
  public String name() {
    return "Sentiment Boost";
  }

  @Override
  public String description() {
    return "Boost a stock's sentiment for several regimes";
  }

  @Override
  public PowerupUsageType usageType() {
    return PowerupUsageType.TARGET_SYMBOL;
  }

  @Override
  public ConsumptionMode consumptionMode() {
    return ConsumptionMode.SINGLE;
  }
}
