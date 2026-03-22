package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SentimentPowerupTest {

  private static final Symbol AAPL = new Symbol("AAPL");

  @ParameterizedTest
  @EnumSource(SentimentTier.class)
  void onActivateReturnsInfluenceSentimentAndEmit(SentimentTier tier) {
    var powerup = new SentimentPowerup(tier, new Random(42));
    powerup.setSymbolTarget(AAPL);

    List<PowerupEffect> effects = powerup.onActivate();

    assertEquals(2, effects.size());
    assertInstanceOf(PowerupEffect.InfluenceSentiment.class, effects.get(0));
    assertInstanceOf(PowerupEffect.Emit.class, effects.get(1));
  }

  @ParameterizedTest
  @EnumSource(SentimentTier.class)
  void influenceHasCorrectSentimentAndValidRanges(SentimentTier tier) {
    int minDelay = tier.minDelay();
    int maxDelay = minDelay + tier.delayRange() - 1;
    int minDuration = tier.minDuration();
    int maxDuration = minDuration + tier.durationRange() - 1;

    for (int seed = 0; seed < 50; seed++) {
      var powerup = new SentimentPowerup(tier, new Random(seed));
      powerup.setSymbolTarget(AAPL);

      var influence = ((PowerupEffect.InfluenceSentiment) powerup.onActivate().get(0)).influence();

      assertEquals(tier.sentiment(), influence.forcedSentiment());
      assertTrue(
          influence.delayRegimes() >= minDelay && influence.delayRegimes() <= maxDelay,
          "delay should be %d-%d but was %d"
              .formatted(minDelay, maxDelay, influence.delayRegimes()));
      assertTrue(
          influence.durationRegimes() >= minDuration && influence.durationRegimes() <= maxDuration,
          "duration should be %d-%d but was %d"
              .formatted(minDuration, maxDuration, influence.durationRegimes()));
    }
  }

  @ParameterizedTest
  @EnumSource(SentimentTier.class)
  void influenceTargetsCorrectSymbol(SentimentTier tier) {
    var powerup = new SentimentPowerup(tier, new Random(42));
    powerup.setSymbolTarget(AAPL);

    var effect = (PowerupEffect.InfluenceSentiment) powerup.onActivate().get(0);
    assertEquals(AAPL, effect.symbol());
  }

  @ParameterizedTest
  @EnumSource(SentimentTier.class)
  void returnsEmptyWithoutTarget(SentimentTier tier) {
    var powerup = new SentimentPowerup(tier, new Random(42));
    assertTrue(powerup.onActivate().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(SentimentTier.class)
  void metadataMatchesTier(SentimentTier tier) {
    var powerup = new SentimentPowerup(tier, new Random(42));
    assertEquals(tier.displayName(), powerup.name());
    assertEquals(tier.description(), powerup.description());
    assertEquals(PowerupUsageType.TARGET_SYMBOL, powerup.usageType());
    assertEquals(ConsumptionMode.SINGLE, powerup.consumptionMode());
  }
}
