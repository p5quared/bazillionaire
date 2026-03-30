package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class DarkPoolTriggerTest {
  private static final PlayerId PLAYER_1 = new PlayerId("player1");
  private static final PlayerId PLAYER_2 = new PlayerId("player2");

  private GameContext twoPlayerContext() {
    return new GameContext(
        10,
        Map.of(
            PLAYER_1,
            new GameEvent.PlayerPortfolio(new Money(100_00), Map.of()),
            PLAYER_2,
            new GameEvent.PlayerPortfolio(new Money(100_00), Map.of())),
        Map.of(new Symbol("AAPL"), new Money(50_00)),
        List.of(),
        Set.of());
  }

  @Test
  void doesNotFireAboveProbability() {
    // Random.nextDouble() with seed 0 returns ~0.73 — above 0.01
    var trigger = new DarkPoolTrigger(0.01, new Random(0));
    List<AwardedPowerup> awards = trigger.evaluate(twoPlayerContext());
    assertTrue(awards.isEmpty());
  }

  @Test
  void firesWhenBelowProbability() {
    var trigger = new DarkPoolTrigger(1.0, new Random(42));
    List<AwardedPowerup> awards = trigger.evaluate(twoPlayerContext());

    assertEquals(1, awards.size());
    assertInstanceOf(DarkPoolPowerup.class, awards.get(0).powerup());
  }

  @Test
  void awardsToRandomPlayer() {
    boolean sawPlayer1 = false;
    boolean sawPlayer2 = false;

    for (int seed = 0; seed < 100; seed++) {
      var trigger = new DarkPoolTrigger(1.0, new Random(seed));
      List<AwardedPowerup> awards = trigger.evaluate(twoPlayerContext());
      if (!awards.isEmpty()) {
        PlayerId recipient = awards.get(0).recipient();
        if (recipient.equals(PLAYER_1)) sawPlayer1 = true;
        if (recipient.equals(PLAYER_2)) sawPlayer2 = true;
      }
    }
    assertTrue(sawPlayer1, "Player 1 should receive at least one award");
    assertTrue(sawPlayer2, "Player 2 should receive at least one award");
  }

  @Test
  void tierDistribution() {
    int standardCount = 0;
    int premiumCount = 0;

    for (int seed = 0; seed < 1000; seed++) {
      var trigger = new DarkPoolTrigger(1.0, new Random(seed));
      List<AwardedPowerup> awards = trigger.evaluate(twoPlayerContext());
      if (!awards.isEmpty()) {
        DarkPoolPowerup powerup = (DarkPoolPowerup) awards.get(0).powerup();
        if (powerup.usageType() == PowerupUsageType.TARGET_SYMBOL) {
          standardCount++;
        } else {
          premiumCount++;
        }
      }
    }

    int total = standardCount + premiumCount;
    double standardRatio = (double) standardCount / total;
    assertTrue(
        standardRatio > 0.75 && standardRatio < 0.95,
        "Expected ~85% STANDARD, got " + (standardRatio * 100) + "%");
  }

  @Test
  void rejectsInvalidProbability() {
    assertThrows(IllegalArgumentException.class, () -> new DarkPoolTrigger(-0.1, new Random()));
    assertThrows(IllegalArgumentException.class, () -> new DarkPoolTrigger(1.1, new Random()));
  }
}
