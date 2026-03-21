package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.port.out.GameFinishedSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameResultListenerTest {

  private GameResultListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(String username, boolean won) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new GameResultListener();
    listener.statsService =
        new PlayerGameStatsService() {
          @Override
          public void recordGame(String username, boolean won) {
            recordedCalls.add(new RecordedCall(username, won));
          }
        };
  }

  @Test
  void winnerIsPlayerWithHighestCashBalance() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), portfolio(500_00));
    players.put(new PlayerId("bob"), portfolio(800_00));
    players.put(new PlayerId("charlie"), portfolio(300_00));

    listener.onGameFinished(new GameId("game1"), snapshot(players));

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertTrue(bobCall.won());

    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertFalse(aliceCall.won());

    var charlieCall =
        recordedCalls.stream().filter(c -> c.username().equals("charlie")).findFirst().get();
    assertFalse(charlieCall.won());
  }

  @Test
  void allPlayersGetRecorded() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), portfolio(100_00));
    players.put(new PlayerId("bob"), portfolio(200_00));

    listener.onGameFinished(new GameId("game1"), snapshot(players));

    assertEquals(2, recordedCalls.size());
  }

  @Test
  void holdingsDoNotAffectWinner() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    // alice has less cash but holds shares
    players.put(
        new PlayerId("alice"),
        new GameEvent.PlayerPortfolio(new Money(100_00), Map.of(new Symbol("AAPL"), 100)));
    // bob has more cash but no shares
    players.put(new PlayerId("bob"), portfolio(200_00));

    listener.onGameFinished(new GameId("game1"), snapshot(players));

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertTrue(bobCall.won());
  }

  private static GameEvent.PlayerPortfolio portfolio(int cashCents) {
    return new GameEvent.PlayerPortfolio(new Money(cashCents), Map.of());
  }

  private static GameFinishedSnapshot snapshot(Map<PlayerId, GameEvent.PlayerPortfolio> players) {
    return new GameFinishedSnapshot(players, Map.of());
  }
}
