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

class GameResultListenerTest {

  private GameResultListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(String username, String gameId, boolean won, int finalCashCents) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new GameResultListener();
    listener.statsService =
        new PlayerGameStatsService() {
          @Override
          public void recordGame(String username, String gameId, boolean won, int finalCashCents) {
            recordedCalls.add(new RecordedCall(username, gameId, won, finalCashCents));
          }
        };
  }

  @Test
  void winnerIsPlayerWithHighestCashBalance() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), portfolio(500_00));
    players.put(new PlayerId("bob"), portfolio(800_00));
    players.put(new PlayerId("charlie"), portfolio(300_00));

    fireGameFinished("game1", players);

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

    fireGameFinished("game1", players);

    assertEquals(2, recordedCalls.size());
  }

  @Test
  void holdingsAffectWinner() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    // alice: $100 cash + 100 shares of AAPL @ $5 = $600 total
    players.put(
        new PlayerId("alice"),
        new GameEvent.PlayerPortfolio(new Money(100_00), Map.of(new Symbol("AAPL"), 100)));
    // bob: $200 cash, no holdings = $200 total
    players.put(new PlayerId("bob"), portfolio(200_00));

    var finalPrices = Map.of(new Symbol("AAPL"), new Money(5_00));
    fireGameFinished("game1", players, finalPrices);

    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertTrue(aliceCall.won());

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertFalse(bobCall.won());
  }

  @Test
  void recordsFinalCashCentsPerPlayer() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), portfolio(500_00));
    players.put(new PlayerId("bob"), portfolio(800_00));

    fireGameFinished("game1", players);

    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertEquals(500_00, aliceCall.finalCashCents());

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertEquals(800_00, bobCall.finalCashCents());
  }

  @Test
  void recordsGameId() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), portfolio(100_00));

    fireGameFinished("my-game", players);

    assertEquals("my-game", recordedCalls.get(0).gameId());
  }

  private void fireGameFinished(String gameId, Map<PlayerId, GameEvent.PlayerPortfolio> players) {
    fireGameFinished(gameId, players, Map.of());
  }

  private void fireGameFinished(
      String gameId,
      Map<PlayerId, GameEvent.PlayerPortfolio> players,
      Map<Symbol, Money> finalPrices) {
    var event = new GameEvent.GameFinished(players, finalPrices, new Money(0));
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(new GameId(gameId), List.of(message));
  }

  private static GameEvent.PlayerPortfolio portfolio(int cashCents) {
    return new GameEvent.PlayerPortfolio(new Money(cashCents), Map.of());
  }
}
