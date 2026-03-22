package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import org.junit.jupiter.api.Test;

class SentimentTriggerTest {

  private final GameContext onePlayerContext =
      new GameContext(
          0,
          Map.of(new PlayerId("p1"), new GameEvent.PlayerPortfolio(new Money(100000), Map.of())),
          Map.of(),
          List.of());

  @Test
  void awardsBoostWhenProbabilityIs1() {
    var trigger =
        new SentimentTrigger(
            1.0, new Random(42), SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR);
    List<AwardedPowerup> awards = trigger.evaluate(onePlayerContext);
    assertEquals(1, awards.size());
    String name = awards.get(0).powerup().name();
    assertTrue(
        name.equals("Sentiment Boost") || name.equals("Sentiment Boost (Major)"),
        "unexpected powerup name: " + name);
  }

  @Test
  void awardsCrashWhenProbabilityIs1() {
    var trigger =
        new SentimentTrigger(
            1.0, new Random(42), SentimentTier.CRASH_MINOR, SentimentTier.CRASH_MAJOR);
    List<AwardedPowerup> awards = trigger.evaluate(onePlayerContext);
    assertEquals(1, awards.size());
    String name = awards.get(0).powerup().name();
    assertTrue(
        name.equals("Sentiment Crash") || name.equals("Sentiment Crash (Major)"),
        "unexpected powerup name: " + name);
  }

  @Test
  void awardsNothingWhenProbabilityIs0() {
    var trigger =
        new SentimentTrigger(
            0.0, new Random(42), SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR);
    assertTrue(trigger.evaluate(onePlayerContext).isEmpty());
  }

  @Test
  void recipientIsFromPlayerList() {
    PlayerId player = new PlayerId("p1");
    var trigger =
        new SentimentTrigger(
            1.0, new Random(42), SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR);
    List<AwardedPowerup> awards = trigger.evaluate(onePlayerContext);
    assertEquals(player, awards.get(0).recipient());
  }

  @Test
  void throwsForInvalidProbability() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SentimentTrigger(
                1.5, new Random(), SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR));
  }
}
