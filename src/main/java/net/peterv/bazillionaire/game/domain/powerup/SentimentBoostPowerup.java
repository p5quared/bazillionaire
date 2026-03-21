package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.ticker.regime.SentimentInfluence;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class SentimentBoostPowerup extends Powerup {
  private final SentimentBoostTier tier;
  private final Random random;
  private Symbol targetSymbol;

  public SentimentBoostPowerup(SentimentBoostTier tier, Random random) {
    super(0);
    this.tier = tier;
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
    int delay = tier.minDelay() + random.nextInt(tier.delayRange());
    int duration = tier.minDuration() + random.nextInt(tier.durationRange());
    var influence = new SentimentInfluence(tier.sentiment(), delay, duration);
    return List.of(
        new PowerupEffect.InfluenceSentiment(targetSymbol, influence),
        new PowerupEffect.Emit(
            GameMessage.broadcast(
                new GameEvent.SentimentBoostActivated(targetSymbol, tier.displayName()))));
  }

  @Override
  public String name() {
    return tier.displayName();
  }

  @Override
  public String description() {
    return tier.description();
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
