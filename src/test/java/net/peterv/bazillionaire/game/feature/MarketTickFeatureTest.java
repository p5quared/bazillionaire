package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

class MarketTickFeatureTest {

  @Test
  void tickEmitsPriceUpdatesAndProgress() {
    var h = GameScenarios.singlePlayerStarted();
    int checkpoint = h.messageCheckpoint();
    h.tick();
    assertHasEventSince(h, GameEvent.TickerTicked.class, checkpoint);
    assertHasEventSince(h, GameEvent.GameTickProgressed.class, checkpoint);
  }

  @Test
  void tickProgressReportsCorrectState() {
    var h = GameScenarios.singlePlayerStarted();
    var result = h.tick();
    assertEquals(1, result.result().tick());
    assertTrue(result.result().ticksRemaining() > 0);
  }

  @Test
  void tickBeforeStartProducesNoMarketEvents() {
    var h = GameScenarios.twoPlayerGame();
    h.join("player1");
    int checkpoint = h.messageCheckpoint();
    h.tick();
    assertHasNoEventSince(h, GameEvent.TickerTicked.class, checkpoint);
  }

  @Test
  void gameFinishesAfterAllTicks() {
    var h = GameScenarios.shortGame(5);
    h.joinAllAndStart();
    h.tickN(5);
    assertGameFinished(h);
  }

  @Test
  void gameFinishedIncludesAllPlayers() {
    var h = GameScenarios.shortGame(3);
    h.joinAllAndStart();
    h.tickN(3);
    var finished = h.eventsOfType(GameEvent.GameFinished.class);
    assertEquals(1, finished.size());
    for (String playerId : h.playerIds()) {
      assertTrue(
          finished.get(0).players().entrySet().stream()
              .anyMatch(e -> e.getKey().value().equals(playerId)),
          "GameFinished should include player " + playerId);
    }
  }

  @Test
  void allTickersReceivePriceUpdatesOnTick() {
    var h = GameScenarios.singlePlayerStarted();
    int checkpoint = h.messageCheckpoint();
    h.tick();
    var ticked = h.eventsOfTypeSince(GameEvent.TickerTicked.class, checkpoint);
    assertEquals(h.symbols().size(), ticked.size());
  }
}
