package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.ticker.regime.MarketSentiment;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class SentimentBoostPowerupTest {

  private static final Symbol AAPL = new Symbol("AAPL");

  @Test
  void onActivateReturnsInfluenceSentimentAndEmit() {
    var powerup = new SentimentBoostPowerup(new Random(42));
    powerup.setSymbolTarget(AAPL);

    List<PowerupEffect> effects = powerup.onActivate();

    assertEquals(2, effects.size());
    assertInstanceOf(PowerupEffect.InfluenceSentiment.class, effects.get(0));
    assertInstanceOf(PowerupEffect.Emit.class, effects.get(1));
  }

  @Test
  void influenceHasBullSentimentAndValidRanges() {
    for (int seed = 0; seed < 50; seed++) {
      var powerup = new SentimentBoostPowerup(new Random(seed));
      powerup.setSymbolTarget(AAPL);

      List<PowerupEffect> effects = powerup.onActivate();
      var influence = ((PowerupEffect.InfluenceSentiment) effects.get(0)).influence();

      assertEquals(MarketSentiment.BULL, influence.forcedSentiment());
      assertTrue(
          influence.delayRegimes() >= 1 && influence.delayRegimes() <= 3,
          "delay should be 1-3 but was " + influence.delayRegimes());
      assertTrue(
          influence.durationRegimes() >= 2 && influence.durationRegimes() <= 5,
          "duration should be 2-5 but was " + influence.durationRegimes());
    }
  }

  @Test
  void influenceTargetsCorrectSymbol() {
    var powerup = new SentimentBoostPowerup(new Random(42));
    powerup.setSymbolTarget(AAPL);

    var effect = (PowerupEffect.InfluenceSentiment) powerup.onActivate().get(0);
    assertEquals(AAPL, effect.symbol());
  }

  @Test
  void returnsEmptyWithoutTarget() {
    var powerup = new SentimentBoostPowerup(new Random(42));
    assertTrue(powerup.onActivate().isEmpty());
  }

  @Test
  void metadata() {
    var powerup = new SentimentBoostPowerup(new Random(42));
    assertEquals("Sentiment Boost", powerup.name());
    assertEquals(PowerupUsageType.TARGET_SYMBOL, powerup.usageType());
    assertEquals(ConsumptionMode.SINGLE, powerup.consumptionMode());
  }
}
