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

class PortfolioStatsListenerTest {

  private PortfolioStatsListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(
      String username, String gameId, long finalPortfolioValueCents, int holdingsCount) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new PortfolioStatsListener();
    listener.portfolioStatsService =
        new PlayerPortfolioStatsService() {
          @Override
          public void recordPortfolio(
              String username, String gameId, long finalPortfolioValueCents, int holdingsCount) {
            recordedCalls.add(
                new RecordedCall(username, gameId, finalPortfolioValueCents, holdingsCount));
          }
        };
  }

  @Test
  void cashOnlyPortfolio() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), new GameEvent.PlayerPortfolio(new Money(500_00), Map.of()));

    fireGameFinished("game1", players, Map.of());

    assertEquals(1, recordedCalls.size());
    assertEquals(500_00, recordedCalls.get(0).finalPortfolioValueCents());
    assertEquals(0, recordedCalls.get(0).holdingsCount());
  }

  @Test
  void holdingsOnlyPortfolio() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(
        new PlayerId("alice"),
        new GameEvent.PlayerPortfolio(new Money(0), Map.of(new Symbol("AAPL"), 10)));
    var prices = Map.of(new Symbol("AAPL"), new Money(50_00));

    fireGameFinished("game1", players, prices);

    assertEquals(500_00, recordedCalls.get(0).finalPortfolioValueCents());
    assertEquals(1, recordedCalls.get(0).holdingsCount());
  }

  @Test
  void mixedPortfolio() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(
        new PlayerId("alice"),
        new GameEvent.PlayerPortfolio(
            new Money(200_00), Map.of(new Symbol("AAPL"), 5, new Symbol("GOOG"), 3)));
    var prices =
        Map.of(
            new Symbol("AAPL"), new Money(100_00),
            new Symbol("GOOG"), new Money(50_00));

    fireGameFinished("game1", players, prices);

    // 200_00 + 5*100_00 + 3*50_00 = 200_00 + 500_00 + 150_00 = 850_00
    assertEquals(850_00, recordedCalls.get(0).finalPortfolioValueCents());
    assertEquals(2, recordedCalls.get(0).holdingsCount());
  }

  @Test
  void allPlayersGetRecorded() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(new PlayerId("alice"), new GameEvent.PlayerPortfolio(new Money(100_00), Map.of()));
    players.put(new PlayerId("bob"), new GameEvent.PlayerPortfolio(new Money(200_00), Map.of()));

    fireGameFinished("game1", players, Map.of());

    assertEquals(2, recordedCalls.size());
  }

  @Test
  void zeroShareHoldingsNotCounted() {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    players.put(
        new PlayerId("alice"),
        new GameEvent.PlayerPortfolio(
            new Money(100_00), Map.of(new Symbol("AAPL"), 5, new Symbol("GOOG"), 0)));
    var prices =
        Map.of(
            new Symbol("AAPL"), new Money(10_00),
            new Symbol("GOOG"), new Money(20_00));

    fireGameFinished("game1", players, prices);

    assertEquals(1, recordedCalls.get(0).holdingsCount());
  }

  private void fireGameFinished(
      String gameId,
      Map<PlayerId, GameEvent.PlayerPortfolio> players,
      Map<Symbol, Money> finalPrices) {
    var event = new GameEvent.GameFinished(players, finalPrices);
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(new GameId(gameId), List.of(message));
  }
}
