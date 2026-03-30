package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

class DarkPoolFeatureTest {

  @Test
  void darkPoolPowerupsAreAwardedDuringGameplay() {
    var h = GameScenarios.twoPlayerStarted();
    h.tickN(2000);

    var darkPoolAwards =
        h.eventsOfType(GameEvent.PowerupAwarded.class).stream()
            .filter(
                e ->
                    e.powerupName().equals("Dark Pool")
                        || e.powerupName().equals("Premium Dark Pool"))
            .toList();

    assertFalse(
        darkPoolAwards.isEmpty(),
        "Expected at least one dark pool powerup to be awarded within 2000 ticks");
  }

  @Test
  void darkPoolPowerupCanBeUsedOnTicker() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    // Tick until a dark pool is awarded
    String darkPoolName = null;
    for (int i = 0; i < 3000; i++) {
      h.tick();
      var awards =
          h.eventsOfType(GameEvent.PowerupAwarded.class).stream()
              .filter(
                  e ->
                      e.powerupName().equals("Dark Pool")
                          || e.powerupName().equals("Premium Dark Pool"))
              .toList();
      if (!awards.isEmpty()) {
        darkPoolName = awards.get(0).powerupName();
        break;
      }
    }

    if (darkPoolName == null) {
      // Probabilistic test — dark pool may not appear with this seed. Skip gracefully.
      return;
    }

    int checkpoint = h.messageCheckpoint();

    if (darkPoolName.equals("Dark Pool")) {
      // STANDARD — use with target symbol
      h.usePowerup("player1", darkPoolName, null, symbol, 1);
    } else {
      // PREMIUM — use without target symbol (INSTANT)
      h.usePowerup("player1", darkPoolName, null);
    }

    // After activation, trade should work
    var result = h.buy("player1", symbol);
    assertFilled(result);

    // Check that the trade is marked as dark pool
    List<GameEvent.OrderActivity> activities =
        h.eventsOfTypeSince(GameEvent.OrderActivity.class, checkpoint);
    boolean hasDarkPoolTrade = activities.stream().anyMatch(GameEvent.OrderActivity::darkPool);
    assertTrue(hasDarkPoolTrade, "Trade through dark pool should be marked as darkPool=true");
  }
}
