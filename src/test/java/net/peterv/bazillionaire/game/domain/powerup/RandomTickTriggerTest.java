package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import org.junit.jupiter.api.Test;

class RandomTickTriggerTest {

  private final GameContext onePlayerContext =
      new GameContext(
          0,
          Map.of(new PlayerId("p1"), new GameEvent.PlayerPortfolio(new Money(100000), Map.of())),
          Map.of(),
          List.of());

  @Test
  void awardsBoostWhenProbabilityIs1() {
    RandomTickTrigger trigger = new RandomTickTrigger(1.0, new Money(50000), new Random(42));
    List<AwardedPowerup> awards = trigger.evaluate(onePlayerContext);
    assertEquals(1, awards.size());
    assertEquals("Cash Boost", awards.get(0).powerup().name());
  }

  @Test
  void awardsNothingWhenProbabilityIs0() {
    RandomTickTrigger trigger = new RandomTickTrigger(0.0, new Money(50000), new Random(42));
    assertTrue(trigger.evaluate(onePlayerContext).isEmpty());
  }

  @Test
  void recipientIsFromPlayerList() {
    PlayerId player = new PlayerId("p1");
    RandomTickTrigger trigger = new RandomTickTrigger(1.0, new Money(50000), new Random(42));
    List<AwardedPowerup> awards = trigger.evaluate(onePlayerContext);
    assertEquals(player, awards.get(0).recipient());
  }

  @Test
  void throwsForInvalidProbability() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new RandomTickTrigger(1.5, new Money(50000), new Random()));
  }
}
