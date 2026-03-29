package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;
import org.junit.jupiter.api.Test;

class PowerupFeatureTest {

  @Test
  void powerupsDropDuringGameplay() {
    var h = GameScenarios.singlePlayerStarted();
    h.tickUntilEvent(GameEvent.PowerupAwarded.class, 2000);
    assertHasEvent(h, GameEvent.PowerupAwarded.class);
  }

  @Test
  void powerupAwardedIsBroadcast() {
    var h = GameScenarios.singlePlayerStarted();
    h.tickUntilEvent(GameEvent.PowerupAwarded.class, 2000);

    var awardedMessages =
        h.messages().stream().filter(m -> m.event() instanceof GameEvent.PowerupAwarded).toList();
    assertFalse(awardedMessages.isEmpty());
    awardedMessages.forEach(GameAssertions::assertIsBroadcast);
  }

  @Test
  void cannotUseUnownedPowerup() {
    var h = GameScenarios.singlePlayerStarted();
    var result = h.usePowerup("player1", "nonexistent", null);
    assertInstanceOf(UsePowerupResult.NotOwned.class, result.result());
  }

  @Test
  void dividendPowerupsAwardedToPlayersHoldingShares() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    // DividendTier.TIER_3 requires 8 shares and 0 hold ticks
    for (int i = 0; i < 8; i++) {
      var result = h.buy("player1", symbol);
      if (!(result.result() instanceof OrderResult.Filled)) {
        h.tick();
        h.buy("player1", symbol);
      }
    }

    // Dividend triggers produce PowerupAwarded events with name "Dividend:SYMBOL"
    h.tickN(200);
    var dividendAwards =
        h.eventsOfType(GameEvent.PowerupAwarded.class).stream()
            .filter(e -> e.powerupName().startsWith("Dividend:"))
            .toList();
    assertFalse(
        dividendAwards.isEmpty(),
        "Expected at least one dividend powerup to be awarded after holding 8 shares for 200 ticks");
  }

  @Test
  void usingDividendPowerupPaysPlayer() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    for (int i = 0; i < 8; i++) {
      var result = h.buy("player1", symbol);
      if (!(result.result() instanceof OrderResult.Filled)) {
        h.tick();
        h.buy("player1", symbol);
      }
    }

    // Tick until dividend is awarded
    String dividendName = null;
    for (int i = 0; i < 200; i++) {
      h.tick();
      var awards =
          h.eventsOfType(GameEvent.PowerupAwarded.class).stream()
              .filter(e -> e.powerupName().startsWith("Dividend:"))
              .toList();
      if (!awards.isEmpty()) {
        dividendName = awards.get(0).powerupName();
        break;
      }
    }
    assertNotNull(dividendName, "Dividend should be awarded within 200 ticks");

    // Use the dividend powerup
    int checkpoint = h.messageCheckpoint();
    h.usePowerup("player1", dividendName, null);
    assertHasEventSince(h, GameEvent.DividendPaid.class, checkpoint);
  }

  @Test
  void sentimentPowerupsAreAwardedDuringGameplay() {
    var h = GameScenarios.singlePlayerStarted();
    // Sentiment powerups are awarded probabilistically, then must be used to activate
    // Just verify they appear as PowerupAwarded events
    h.tickN(2000);
    var sentimentAwards =
        h.eventsOfType(GameEvent.PowerupAwarded.class).stream()
            .filter(
                e ->
                    e.powerupName().toLowerCase().contains("boost")
                        || e.powerupName().toLowerCase().contains("crash"))
            .toList();
    assertFalse(
        sentimentAwards.isEmpty(),
        "Expected at least one sentiment powerup to be awarded within 2000 ticks");
  }
}
