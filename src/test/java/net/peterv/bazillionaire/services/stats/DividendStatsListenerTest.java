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

class DividendStatsListenerTest {

  private DividendStatsListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(String username, String gameId, int count, int totalCents) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new DividendStatsListener();
    listener.dividendStatsService =
        new PlayerDividendStatsService() {
          @Override
          public void recordDividends(String username, String gameId, int count, int totalCents) {
            recordedCalls.add(new RecordedCall(username, gameId, count, totalCents));
          }
        };
  }

  @Test
  void accumulatesDividendCountAndCash() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");

    fireDividendPaid(gameId, alice, 50_00);
    fireDividendPaid(gameId, alice, 30_00);
    fireDividendPaid(gameId, alice, 20_00);
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.size());
    var call = recordedCalls.get(0);
    assertEquals("alice", call.username());
    assertEquals("game1", call.gameId());
    assertEquals(3, call.count());
    assertEquals(100_00, call.totalCents());
  }

  @Test
  void tracksMultiplePlayersIndependently() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var bob = new PlayerId("bob");

    fireDividendPaid(gameId, alice, 50_00);
    fireDividendPaid(gameId, bob, 75_00);
    fireDividendPaid(gameId, alice, 25_00);
    fireGameFinished(gameId);

    assertEquals(2, recordedCalls.size());

    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertEquals(2, aliceCall.count());
    assertEquals(75_00, aliceCall.totalCents());

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertEquals(1, bobCall.count());
    assertEquals(75_00, bobCall.totalCents());
  }

  @Test
  void cleansUpGameEntryAfterPersist() {
    var gameId = new GameId("game1");
    fireDividendPaid(gameId, new PlayerId("alice"), 50_00);
    fireGameFinished(gameId);

    recordedCalls.clear();

    // Second game finish for same ID should not produce any calls
    fireGameFinished(gameId);
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void noDividendsProducesNoRecords() {
    fireGameFinished(new GameId("game1"));
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void tracksMultipleGamesIndependently() {
    var game1 = new GameId("game1");
    var game2 = new GameId("game2");
    var alice = new PlayerId("alice");

    fireDividendPaid(game1, alice, 50_00);
    fireDividendPaid(game2, alice, 100_00);
    fireGameFinished(game1);

    assertEquals(1, recordedCalls.size());
    assertEquals("game1", recordedCalls.get(0).gameId());
    assertEquals(50_00, recordedCalls.get(0).totalCents());

    recordedCalls.clear();
    fireGameFinished(game2);

    assertEquals(1, recordedCalls.size());
    assertEquals("game2", recordedCalls.get(0).gameId());
    assertEquals(100_00, recordedCalls.get(0).totalCents());
  }

  private void fireDividendPaid(GameId gameId, PlayerId playerId, int cents) {
    var event = new GameEvent.DividendPaid(playerId, new Symbol("AAPL"), new Money(cents), "TIER1");
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
