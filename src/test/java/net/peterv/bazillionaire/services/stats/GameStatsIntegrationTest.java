package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.feature.GameTestHarness;
import net.peterv.bazillionaire.game.port.out.GameEventListener;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GameStatsIntegrationTest {

  @Inject GameResultListener gameResultListener;
  @Inject DividendStatsListener dividendStatsListener;
  @Inject TradeStatsListener tradeStatsListener;
  @Inject PowerupStatsListener powerupStatsListener;
  @Inject PortfolioStatsListener portfolioStatsListener;

  @Test
  void fullGamePersistsAllStats() {
    var harness =
        GameTestHarness.builder().players("alice", "bob").duration(120).build().joinAllAndStart();

    var firstSymbol = harness.symbols().get(0);

    // Make some trades
    harness.buy("alice", firstSymbol);
    harness.buy("alice", firstSymbol);
    harness.buy("bob", firstSymbol);
    harness.sell("bob", firstSymbol);

    // Tick to completion
    harness.tickUntilFinished(200);

    // Feed all messages to listeners
    var gameId = new GameId(harness.gameId());
    List<GameMessage> messages = harness.messages();
    List<GameEventListener> listeners =
        List.of(
            gameResultListener,
            dividendStatsListener,
            tradeStatsListener,
            powerupStatsListener,
            portfolioStatsListener);
    for (var listener : listeners) {
      listener.onGameEvents(gameId, messages);
    }

    // Verify game results persisted
    var gameResults = PlayerGameResult.findByGameId(harness.gameId());
    assertEquals(2, gameResults.size(), "Both players should have game results");
    assertTrue(gameResults.stream().anyMatch(r -> r.won), "There should be a winner");

    // Verify portfolio results persisted
    var portfolioResults = PlayerPortfolioResult.findByGameId(harness.gameId());
    assertEquals(2, portfolioResults.size(), "Both players should have portfolio results");
    for (var pr : portfolioResults) {
      assertTrue(pr.finalPortfolioValueCents > 0, "Portfolio value should be positive");
    }

    // Verify trade results persisted (we made trades above)
    var tradeResults = PlayerTradeResult.findByGameId(harness.gameId());
    assertFalse(tradeResults.isEmpty(), "At least one player should have trade results");
    var aliceTrades =
        tradeResults.stream().filter(t -> t.username.equals("alice")).findFirst().orElseThrow();
    assertTrue(aliceTrades.totalBuys >= 2, "Alice made at least 2 buys");

    // Verify GameFinished was processed
    var finishedEvents = harness.eventsOfType(GameEvent.GameFinished.class);
    assertEquals(1, finishedEvents.size(), "Game should have finished exactly once");
  }

  @Test
  void winnerDeterminedByPortfolioValueNotCash() {
    var harness =
        GameTestHarness.builder().players("pvp1", "pvp2").duration(60).build().joinAllAndStart();

    var symbol = harness.symbols().get(0);

    // pvp1 buys aggressively — spends cash but holds stock
    harness.buy("pvp1", symbol);
    harness.buy("pvp1", symbol);
    harness.buy("pvp1", symbol);

    harness.tickUntilFinished(200);

    var gameId = new GameId(harness.gameId());
    List<GameMessage> messages = harness.messages();

    gameResultListener.onGameEvents(gameId, messages);
    portfolioStatsListener.onGameEvents(gameId, messages);

    var gameResults = PlayerGameResult.findByGameId(harness.gameId());
    var portfolioResults = PlayerPortfolioResult.findByGameId(harness.gameId());

    // Verify winner has highest portfolio value, not necessarily highest cash
    var winner = gameResults.stream().filter(r -> r.won).findFirst().orElseThrow();
    var winnerPortfolio =
        portfolioResults.stream()
            .filter(p -> p.username.equals(winner.username))
            .findFirst()
            .orElseThrow();
    var loserPortfolio =
        portfolioResults.stream()
            .filter(p -> !p.username.equals(winner.username))
            .findFirst()
            .orElseThrow();

    assertTrue(
        winnerPortfolio.finalPortfolioValueCents >= loserPortfolio.finalPortfolioValueCents,
        "Winner should have >= portfolio value than loser");
  }
}
