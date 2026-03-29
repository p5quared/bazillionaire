package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

class FullGameFeatureTest {

  @Test
  void completeGameLifecycle() {
    var h = GameScenarios.shortGame(10);

    // Players join
    assertJoined(h.join("player1"));
    assertAllReady(h.join("player2"));
    h.start();

    assertHasEvent(h, GameEvent.GameState.class);

    String symbol = h.symbols().get(0);

    // Player 1 buys
    assertFilled(h.buy("player1", symbol));
    h.tickN(3);

    // Player 2 buys
    assertFilled(h.buy("player2", symbol));
    h.tickN(7);

    // Game should be finished
    assertGameFinished(h);

    // Finished event includes both players
    var finished = h.eventsOfType(GameEvent.GameFinished.class);
    assertEquals(1, finished.size());
    assertTrue(
        finished.get(0).players().entrySet().stream()
            .anyMatch(e -> e.getKey().value().equals("player1")));
    assertTrue(
        finished.get(0).players().entrySet().stream()
            .anyMatch(e -> e.getKey().value().equals("player2")));
  }

  @Test
  void twoPlayersTradeSameStock() {
    var h = GameScenarios.twoPlayerStarted();
    String symbol = h.symbols().get(0);

    // Both players buy
    assertFilled(h.buy("player1", symbol));
    assertFilled(h.buy("player2", symbol));

    h.tick();

    // Both players sell
    assertFilled(h.sell("player1", symbol));
    assertFilled(h.sell("player2", symbol));
  }

  @Test
  void playersCantSeeEachOthersOrderFills() {
    var h = GameScenarios.twoPlayerStarted();
    String symbol = h.symbols().get(0);

    int checkpoint = h.messageCheckpoint();
    h.buy("player1", symbol);

    // OrderFilled should only be private to player1
    var player2Fills =
        h.privateEventsFor("player2", GameEvent.OrderFilled.class).stream()
            .filter(
                e ->
                    h.messagesSince(checkpoint).stream()
                        .filter(m -> m.event() == e)
                        .findFirst()
                        .isPresent())
            .toList();
    assertTrue(player2Fills.isEmpty(), "Player 2 should not see player 1's order fills");
  }

  @Test
  void marketProgressesThroughEntireGame() {
    var h = GameScenarios.shortGame(20);
    h.joinAllAndStart();

    var symbols = h.symbols();
    assertFalse(symbols.isEmpty());

    // Let the entire game play out
    h.tickN(20);
    assertGameFinished(h);

    // Every ticker should have received price updates
    var ticked = h.eventsOfType(GameEvent.TickerTicked.class);
    for (String symbol : symbols) {
      assertTrue(
          ticked.stream().anyMatch(t -> t.symbol().value().equals(symbol)),
          "Ticker " + symbol + " should have received at least one price update");
    }
  }
}
