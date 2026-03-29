package net.peterv.bazillionaire.game.domain.ticker.regime;

import java.util.Random;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.types.Money;

public class DefaultRegimeFactory implements RegimeFactory {

  private final Random random;
  private final MarketCap marketCap;

  public DefaultRegimeFactory(Random random, MarketCap marketCap) {
    this.random = random;
    this.marketCap = marketCap;
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
    return switch (marketCap) {
      case STARTUP -> {
        if (roll < 0.30) yield MarketSentiment.FLAT;
        if (roll < 0.65) yield MarketSentiment.BULL;
        yield MarketSentiment.BEAR;
      }
      case MID_CAP -> {
        if (roll < 0.50) yield MarketSentiment.FLAT;
        if (roll < 0.80) yield MarketSentiment.BULL;
        yield MarketSentiment.BEAR;
      }
      case BLUE_CHIP -> {
        if (roll < 0.65) yield MarketSentiment.FLAT;
        if (roll < 0.90) yield MarketSentiment.BULL;
        yield MarketSentiment.BEAR;
      }
    };
  }

  private RegimeStrategy createBullRegime(Money startPrice) {
    return switch (marketCap) {
      case STARTUP -> {
        if (random.nextDouble() < 0.60) {
          // Explosive — startups boom hard
          int duration = 5 + random.nextInt(11);
          double gain = 0.50 + random.nextDouble() * 1.00;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double curvature = 2.0 + random.nextDouble() * 4.0;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        } else {
          int duration = 15 + random.nextInt(16);
          double gain = 0.15 + random.nextDouble() * 0.25;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double steepness = 4.0 + random.nextDouble() * 6.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        }
      }
      case MID_CAP -> {
        if (random.nextDouble() < 0.30) {
          int duration = 8 + random.nextInt(13);
          double gain = 0.35 + random.nextDouble() * 0.65;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double curvature = 1.0 + random.nextDouble() * 4.0;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        } else {
          int duration = 20 + random.nextInt(21);
          double gain = 0.10 + random.nextDouble() * 0.20;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double steepness = 4.0 + random.nextDouble() * 8.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        }
      }
      case BLUE_CHIP -> {
        if (random.nextDouble() < 0.10) {
          // Rare explosive — blue chips rarely spike
          int duration = 12 + random.nextInt(14);
          double gain = 0.15 + random.nextDouble() * 0.25;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double curvature = 0.5 + random.nextDouble() * 2.0;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        } else {
          // Slow, steady growth
          int duration = 30 + random.nextInt(31);
          double gain = 0.05 + random.nextDouble() * 0.10;
          Money endPrice = scalePrice(startPrice, 1.0 + gain);
          double steepness = 3.0 + random.nextDouble() * 5.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        }
      }
    };
  }

  private RegimeStrategy createStrongBullRegime(Money startPrice) {
    return switch (marketCap) {
      case STARTUP -> {
        int duration = 8 + random.nextInt(13);
        double gain = 0.80 + random.nextDouble() * 1.20;
        Money endPrice = scalePrice(startPrice, 1.0 + gain);
        double curvature = 3.0 + random.nextDouble() * 4.0;
        yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
      }
      case MID_CAP -> {
        int duration = 10 + random.nextInt(16);
        double gain = 0.60 + random.nextDouble() * 0.90;
        Money endPrice = scalePrice(startPrice, 1.0 + gain);
        double curvature = 2.0 + random.nextDouble() * 4.0;
        yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
      }
      case BLUE_CHIP -> {
        int duration = 15 + random.nextInt(16);
        double gain = 0.30 + random.nextDouble() * 0.50;
        Money endPrice = scalePrice(startPrice, 1.0 + gain);
        double curvature = 1.0 + random.nextDouble() * 3.0;
        yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
      }
    };
  }

  private RegimeStrategy createFlatRegime(Money startPrice) {
    return switch (marketCap) {
      case STARTUP -> {
        int duration = 15 + random.nextInt(21);
        double change = -0.12 + random.nextDouble() * 0.32;
        Money endPrice = scalePrice(startPrice, 1.0 + change);
        if (Math.abs(change) < 0.05) {
          int amplitudeCents =
              Math.max(1, (int) (startPrice.cents() * (0.08 + random.nextDouble() * 0.10)));
          int direction = random.nextBoolean() ? 1 : -1;
          yield new CycleRegimeStrategy(startPrice, amplitudeCents, duration, direction);
        } else {
          int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
          int stepCents = Math.max(1, deltaCents / duration);
          int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
          yield new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
        }
      }
      case MID_CAP -> {
        int duration = 25 + random.nextInt(36);
        double change = -0.08 + random.nextDouble() * 0.23;
        Money endPrice = scalePrice(startPrice, 1.0 + change);
        if (Math.abs(change) < 0.03) {
          int amplitudeCents =
              Math.max(1, (int) (startPrice.cents() * (0.05 + random.nextDouble() * 0.10)));
          int direction = random.nextBoolean() ? 1 : -1;
          yield new CycleRegimeStrategy(startPrice, amplitudeCents, duration, direction);
        } else {
          int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
          int stepCents = Math.max(1, deltaCents / duration);
          int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
          yield new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
        }
      }
      case BLUE_CHIP -> {
        int duration = 40 + random.nextInt(41);
        double change = -0.03 + random.nextDouble() * 0.11;
        Money endPrice = scalePrice(startPrice, 1.0 + change);
        if (Math.abs(change) < 0.02) {
          int amplitudeCents =
              Math.max(1, (int) (startPrice.cents() * (0.02 + random.nextDouble() * 0.06)));
          int direction = random.nextBoolean() ? 1 : -1;
          yield new CycleRegimeStrategy(startPrice, amplitudeCents, duration, direction);
        } else {
          int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
          int stepCents = Math.max(1, deltaCents / duration);
          int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
          yield new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
        }
      }
    };
  }

  private RegimeStrategy createBearRegime(Money startPrice) {
    return switch (marketCap) {
      case STARTUP -> {
        if (random.nextDouble() < 0.50) {
          // Gradual decline
          int duration = 30 + random.nextInt(31);
          double loss = 0.10 + random.nextDouble() * 0.20;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double steepness = 4.0 + random.nextDouble() * 8.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        } else {
          // Steep crash — startups fall fast
          int duration = 5 + random.nextInt(8);
          double loss = 0.25 + random.nextDouble() * 0.20;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double curvature = 2.0 + random.nextDouble() * 4.0;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        }
      }
      case MID_CAP -> {
        if (random.nextDouble() < 0.30) {
          int duration = 50 + random.nextInt(51);
          double loss = 0.05 + random.nextDouble() * 0.15;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double steepness = 4.0 + random.nextDouble() * 8.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        } else {
          int duration = 8 + random.nextInt(13);
          double loss = 0.25 + random.nextDouble() * 0.25;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double curvature = 1.0 + random.nextDouble() * 4.0;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        }
      }
      case BLUE_CHIP -> {
        if (random.nextDouble() < 0.70) {
          // Mostly gradual — blue chips decline slowly
          int duration = 60 + random.nextInt(61);
          double loss = 0.03 + random.nextDouble() * 0.07;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double steepness = 4.0 + random.nextDouble() * 8.0;
          yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
        } else {
          int duration = 15 + random.nextInt(16);
          double loss = 0.10 + random.nextDouble() * 0.15;
          Money endPrice = scalePrice(startPrice, 1.0 - loss);
          double curvature = 0.5 + random.nextDouble() * 2.5;
          yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
        }
      }
    };
  }

  private Money scalePrice(Money price, double factor) {
    return new Money(Math.max(1, (int) (price.cents() * factor)));
  }
}
