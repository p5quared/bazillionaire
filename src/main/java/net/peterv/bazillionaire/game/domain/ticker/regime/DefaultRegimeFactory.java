package net.peterv.bazillionaire.game.domain.ticker.regime;

import java.util.Random;
import net.peterv.bazillionaire.game.domain.types.Money;

public class DefaultRegimeFactory implements RegimeFactory {

  private final Random random;

  public DefaultRegimeFactory(Random random) {
    this.random = random;
  }

  @Override
  public RegimeStrategy nextRegime(Money lastPrice) {
    return nextRegime(lastPrice, pickSentiment());
  }

  @Override
  public RegimeStrategy nextRegime(Money lastPrice, MarketSentiment forcedSentiment) {
    return switch (forcedSentiment) {
      case BULL -> createBullRegime(lastPrice);
      case STRONG_BULL -> createStrongBullRegime(lastPrice);
      case FLAT -> createFlatRegime(lastPrice);
      case BEAR -> createBearRegime(lastPrice);
    };
  }

  private MarketSentiment pickSentiment() {
    double roll = random.nextDouble();
    if (roll < 0.50) return MarketSentiment.FLAT;
    if (roll < 0.80) return MarketSentiment.BULL;
    return MarketSentiment.BEAR;
  }

  private RegimeStrategy createBullRegime(Money startPrice) {
    if (random.nextDouble() < 0.30) {
      // Strong/explosive
      int duration = 8 + random.nextInt(13);
      double gain = 0.35 + random.nextDouble() * 0.65;
      Money endPrice = scalePrice(startPrice, 1.0 + gain);
      double curvature = 1.0 + random.nextDouble() * 4.0;
      return new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
    } else {
      // Moderate
      int duration = 20 + random.nextInt(21);
      double gain = 0.10 + random.nextDouble() * 0.20;
      Money endPrice = scalePrice(startPrice, 1.0 + gain);
      double steepness = 4.0 + random.nextDouble() * 8.0;
      return new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
    }
  }

  private RegimeStrategy createStrongBullRegime(Money startPrice) {
    int duration = 10 + random.nextInt(16);
    double gain = 0.60 + random.nextDouble() * 0.90;
    Money endPrice = scalePrice(startPrice, 1.0 + gain);
    double curvature = 2.0 + random.nextDouble() * 4.0;
    return new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
  }

  private RegimeStrategy createFlatRegime(Money startPrice) {
    int duration = 25 + random.nextInt(36);
    double change = -0.08 + random.nextDouble() * 0.23;
    Money endPrice = scalePrice(startPrice, 1.0 + change);
    if (Math.abs(change) < 0.03) {
      int amplitudeCents =
          Math.max(1, (int) (startPrice.cents() * (0.05 + random.nextDouble() * 0.10)));
      int direction = random.nextBoolean() ? 1 : -1;
      return new CycleRegimeStrategy(startPrice, amplitudeCents, duration, direction);
    } else {
      int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
      int stepCents = Math.max(1, deltaCents / duration);
      int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
      return new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
    }
  }

  private RegimeStrategy createBearRegime(Money startPrice) {
    if (random.nextDouble() < 0.30) {
      // Long gradual
      int duration = 50 + random.nextInt(51);
      double loss = 0.05 + random.nextDouble() * 0.15;
      Money endPrice = scalePrice(startPrice, 1.0 - loss);
      double steepness = 4.0 + random.nextDouble() * 8.0;
      return new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
    } else {
      // Steep short
      int duration = 8 + random.nextInt(13);
      double loss = 0.25 + random.nextDouble() * 0.25;
      Money endPrice = scalePrice(startPrice, 1.0 - loss);
      double curvature = 1.0 + random.nextDouble() * 4.0;
      return new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
    }
  }

  private Money scalePrice(Money price, double factor) {
    return new Money(Math.max(1, (int) (price.cents() * factor)));
  }
}
