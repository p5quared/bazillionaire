package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PowerupStatsListenerTest {

  private PowerupStatsListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(
      String username,
      String gameId,
      int powerupsReceived,
      int powerupsUsed,
      int timesFrozen,
      int darkPoolUses) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new PowerupStatsListener();
    listener.powerupStatsService =
        new PlayerPowerupStatsService() {
          @Override
          public void recordPowerups(
              String username,
              String gameId,
              int powerupsReceived,
              int powerupsUsed,
              int timesFrozen,
              int darkPoolUses) {
            recordedCalls.add(
                new RecordedCall(
                    username, gameId, powerupsReceived, powerupsUsed, timesFrozen, darkPoolUses));
          }
        };
  }

  @Test
  void countsPowerupsReceived() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");

    firePowerupAwarded(gameId, alice);
    firePowerupAwarded(gameId, alice);
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.size());
    assertEquals(2, recordedCalls.get(0).powerupsReceived());
  }

  @Test
  void countsPowerupsUsed() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");

    firePowerupActivated(gameId, alice);
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.get(0).powerupsUsed());
  }

  @Test
  void countsTimesFrozen() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");

    fireFreezeStarted(gameId, alice);
    fireFreezeStarted(gameId, alice);
    fireGameFinished(gameId);

    assertEquals(2, recordedCalls.get(0).timesFrozen());
  }

  @Test
  void countsDarkPoolUses() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");

    fireDarkPoolActivated(gameId, alice);
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.get(0).darkPoolUses());
  }

  @Test
  void tracksMultiplePlayersIndependently() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var bob = new PlayerId("bob");

    firePowerupAwarded(gameId, alice);
    firePowerupAwarded(gameId, bob);
    firePowerupAwarded(gameId, bob);
    fireFreezeStarted(gameId, alice);
    fireGameFinished(gameId);

    assertEquals(2, recordedCalls.size());
    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertEquals(1, aliceCall.powerupsReceived());
    assertEquals(1, aliceCall.timesFrozen());

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertEquals(2, bobCall.powerupsReceived());
    assertEquals(0, bobCall.timesFrozen());
  }

  @Test
  void cleansUpGameEntryAfterPersist() {
    var gameId = new GameId("game1");
    firePowerupAwarded(gameId, new PlayerId("alice"));
    fireGameFinished(gameId);

    recordedCalls.clear();
    fireGameFinished(gameId);
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void noEventsProducesNoRecords() {
    fireGameFinished(new GameId("game1"));
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void tracksMultipleGamesIndependently() {
    var game1 = new GameId("game1");
    var game2 = new GameId("game2");
    var alice = new PlayerId("alice");

    firePowerupAwarded(game1, alice);
    firePowerupAwarded(game2, alice);
    firePowerupAwarded(game2, alice);
    fireGameFinished(game1);

    assertEquals(1, recordedCalls.size());
    assertEquals(1, recordedCalls.get(0).powerupsReceived());

    recordedCalls.clear();
    fireGameFinished(game2);

    assertEquals(1, recordedCalls.size());
    assertEquals(2, recordedCalls.get(0).powerupsReceived());
  }

  private void firePowerupAwarded(GameId gameId, PlayerId recipient) {
    var event = new GameEvent.PowerupAwarded(recipient, "TestPowerup", "desc", "INSTANT", "SINGLE");
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void firePowerupActivated(GameId gameId, PlayerId user) {
    var event = new GameEvent.PowerupActivated(user, "TestPowerup");
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void fireFreezeStarted(GameId gameId, PlayerId frozenPlayer) {
    var event = new GameEvent.FreezeStarted(frozenPlayer, 10);
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void fireDarkPoolActivated(GameId gameId, PlayerId player) {
    var event = new GameEvent.DarkPoolActivated(player, "TIER1", new Symbol("AAPL"), 3, 10);
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void fireGameFinished(GameId gameId) {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    var event = new GameEvent.GameFinished(players, Map.of(), new Money(0));
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }
}
