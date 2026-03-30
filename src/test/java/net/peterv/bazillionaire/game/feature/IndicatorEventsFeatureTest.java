package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

class IndicatorEventsFeatureTest {

  @Test
  void tickEmitsMarketIndicatorsBroadcast() {
    var h = GameTestHarness.builder().players("p1", "p2").build().joinAllAndStart();

    h.tick();

    var indicators = h.broadcastEventsOfType(GameEvent.MarketIndicators.class);
    assertFalse(indicators.isEmpty(), "Should emit MarketIndicators on tick");

    var latest = indicators.getLast();
    for (String symbol : h.symbols()) {
      var bubble =
          latest.bubbles().entrySet().stream()
              .filter(e -> e.getKey().value().equals(symbol))
              .findFirst()
              .orElseThrow();
      assertTrue(bubble.getValue().threshold() > 0, "Threshold should be positive");
      assertTrue(bubble.getValue().factor() >= 0, "Factor should be non-negative");
    }
  }

  @Test
  void tickEmitsLiquidityUpdatePerPlayer() {
    var h = GameTestHarness.builder().players("p1", "p2").build().joinAllAndStart();

    h.tick();

    var p1Updates = h.privateEventsFor("p1", GameEvent.LiquidityUpdate.class);
    var p2Updates = h.privateEventsFor("p2", GameEvent.LiquidityUpdate.class);
    assertFalse(p1Updates.isEmpty(), "Should emit LiquidityUpdate for p1");
    assertFalse(p2Updates.isEmpty(), "Should emit LiquidityUpdate for p2");

    var p1Latest = p1Updates.getLast();
    for (String symbol : h.symbols()) {
      var liq =
          p1Latest.liquidity().entrySet().stream()
              .filter(e -> e.getKey().value().equals(symbol))
              .findFirst()
              .orElseThrow();
      assertTrue(liq.getValue().max() > 0, "Max should be positive");
      assertEquals(liq.getValue().max(), liq.getValue().remaining(), "Should start at max");
    }
  }

  @Test
  void liquidityDecreasesAfterTrade() {
    var h = GameTestHarness.builder().players("p1", "p2").build().joinAllAndStart();
    h.tick();
    var symbol = h.symbols().getFirst();

    h.buy("p1", symbol);
    h.tick();

    var updates = h.privateEventsFor("p1", GameEvent.LiquidityUpdate.class);
    var latest = updates.getLast();
    var liq =
        latest.liquidity().entrySet().stream()
            .filter(e -> e.getKey().value().equals(symbol))
            .findFirst()
            .orElseThrow();
    assertTrue(
        liq.getValue().remaining() < liq.getValue().max(),
        "Remaining should be less than max after trading");
  }

  @Test
  void bubbleFactorIncreasesAfterTrades() {
    var h = GameTestHarness.builder().players("p1", "p2").build().joinAllAndStart();
    h.tick();
    var symbol = h.symbols().getFirst();

    for (int i = 0; i < 5; i++) {
      h.buy("p1", symbol);
    }
    h.tick();

    var indicators = h.broadcastEventsOfType(GameEvent.MarketIndicators.class);
    var latest = indicators.getLast();
    var bubble =
        latest.bubbles().entrySet().stream()
            .filter(e -> e.getKey().value().equals(symbol))
            .findFirst()
            .orElseThrow();
    assertTrue(bubble.getValue().factor() > 0, "Bubble factor should increase after trades");
  }

  @Test
  void rejoinInProgressGameReceivesIndicators() {
    var h = GameTestHarness.builder().players("p1", "p2").build().joinAllAndStart();
    h.tick();

    var checkpoint = h.messageCheckpoint();
    h.join("p1"); // rejoin in-progress game

    assertHasEventSince(h, GameEvent.MarketIndicators.class, checkpoint);
    var liqUpdates =
        h.messagesSince(checkpoint).stream()
            .filter(
                m ->
                    m.event() instanceof GameEvent.LiquidityUpdate
                        && m.audience()
                            instanceof net.peterv.bazillionaire.game.domain.types.Audience.Only only
                        && only.playerId()
                            .equals(new net.peterv.bazillionaire.game.domain.types.PlayerId("p1")))
            .toList();
    assertFalse(liqUpdates.isEmpty(), "Rejoining player should receive LiquidityUpdate");
  }
}
