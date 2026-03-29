package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

class JoinAndStartFeatureTest {

  @Test
  void singlePlayerJoinsAndGameBecomesReady() {
    var h = GameTestHarness.builder().players("player1").build();
    assertAllReady(h.join("player1"));
  }

  @Test
  void twoPlayersJoinSequentially() {
    var h = GameScenarios.twoPlayerGame();
    assertJoined(h.join("player1"));
    assertAllReady(h.join("player2"));
  }

  @Test
  void joiningTwiceIsRejected() {
    var h = GameScenarios.twoPlayerGame();
    h.join("player1");
    assertAlreadyReady(h.join("player1"));
  }

  @Test
  void unknownPlayerCannotJoin() {
    var h = GameScenarios.twoPlayerGame();
    assertInvalidJoin(h.join("stranger"));
  }

  @Test
  void startingGameBroadcastsGameState() {
    var h = GameScenarios.twoPlayerGame();
    h.joinAll();
    int checkpoint = h.messageCheckpoint();
    h.start();
    assertHasEventSince(h, GameEvent.GameState.class, checkpoint);
    assertHasEventSince(h, GameEvent.PlayersState.class, checkpoint);
  }

  @Test
  void rejoinAfterStartReturnsGameInProgress() {
    var h = GameScenarios.twoPlayerStarted();
    assertGameInProgress(h.join("player1"));
  }

  @Test
  void rejoinAfterStartSendsPrivateGameState() {
    var h = GameScenarios.twoPlayerStarted();
    int checkpoint = h.messageCheckpoint();
    h.join("player1");
    assertHasEventSince(h, GameEvent.GameState.class, checkpoint);
    assertHasPrivateEventFor(h, "player1", GameEvent.GameState.class);
  }
}
