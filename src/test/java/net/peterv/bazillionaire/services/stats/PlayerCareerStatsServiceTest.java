package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PlayerCareerStatsServiceTest {

  @Inject PlayerCareerStatsService careerStatsService;
  @Inject PlayerGameStatsService gameStatsService;
  @Inject PlayerTradeStatsService tradeStatsService;
  @Inject PlayerPowerupStatsService powerupStatsService;
  @Inject PlayerPortfolioStatsService portfolioStatsService;
  @Inject PlayerDividendStatsService dividendStatsService;

  private String uniqueUsername() {
    return "test-" + UUID.randomUUID();
  }

  private String uniqueGameId() {
    return "game-" + UUID.randomUUID();
  }

  @Test
  void returnsEmptyForUnknownPlayer() {
    var result = careerStatsService.getCareerStats("nonexistent-" + UUID.randomUUID());
    assertTrue(result.isEmpty());
  }

  @Test
  void singleGameWithAllStats() {
    var username = uniqueUsername();
    var gameId = uniqueGameId();

    gameStatsService.recordGame(username, gameId, true, 500_00);
    tradeStatsService.recordTrades(username, gameId, 5, 3, 800_00, 1);
    powerupStatsService.recordPowerups(username, gameId, 4, 2, 1, 1);
    portfolioStatsService.recordPortfolio(username, gameId, 1200_00, 1000_00, 3);
    dividendStatsService.recordDividends(username, gameId, 2, 50_00);

    var stats = careerStatsService.getCareerStats(username).orElseThrow();
    assertEquals(username, stats.username());
    assertEquals(1, stats.gamesPlayed());
    assertEquals(1, stats.wins());
    assertEquals(1.0, stats.winRate(), 0.001);
    assertEquals(200_00, stats.totalEarningsCents());
    assertEquals(200_00, stats.bestGameEarningsCents());
    assertEquals(8, stats.totalTradesMade());
    assertEquals(1, stats.totalOrdersBlocked());
    assertEquals(4, stats.totalPowerupsReceived());
    assertEquals(2, stats.totalPowerupsUsed());
    assertEquals(2, stats.totalDividendsCollected());
    assertEquals(50_00, stats.totalDividendCashCents());
    assertEquals(1, stats.timesFrozen());
    assertEquals(1, stats.darkPoolUses());
  }

  @Test
  void multipleGamesAggregateCorrectly() {
    var username = uniqueUsername();
    var gameId1 = uniqueGameId();
    var gameId2 = uniqueGameId();

    gameStatsService.recordGame(username, gameId1, true, 500_00);
    gameStatsService.recordGame(username, gameId2, false, 300_00);
    portfolioStatsService.recordPortfolio(username, gameId1, 800_00, 500_00, 2);
    portfolioStatsService.recordPortfolio(username, gameId2, 400_00, 500_00, 1);
    tradeStatsService.recordTrades(username, gameId1, 3, 2, 500_00, 0);
    tradeStatsService.recordTrades(username, gameId2, 1, 1, 200_00, 1);

    var stats = careerStatsService.getCareerStats(username).orElseThrow();
    assertEquals(2, stats.gamesPlayed());
    assertEquals(1, stats.wins());
    assertEquals(0.5, stats.winRate(), 0.001);
    assertEquals(200_00, stats.totalEarningsCents());
    assertEquals(300_00, stats.bestGameEarningsCents());
    assertEquals(7, stats.totalTradesMade());
    assertEquals(1, stats.totalOrdersBlocked());
  }

  @Test
  void missingStatCategoriesStillWork() {
    var username = uniqueUsername();
    var gameId = uniqueGameId();

    // Only game result, no trades/powerups/dividends/portfolio
    gameStatsService.recordGame(username, gameId, false, 200_00);

    var stats = careerStatsService.getCareerStats(username).orElseThrow();
    assertEquals(1, stats.gamesPlayed());
    assertEquals(0, stats.wins());
    assertEquals(0, stats.totalTradesMade());
    assertEquals(0, stats.totalPowerupsReceived());
    assertEquals(0, stats.totalDividendsCollected());
    assertEquals(0, stats.totalEarningsCents());
    assertEquals(0, stats.bestGameEarningsCents());
  }

  @Test
  void allLossesReportsLeastNegativeAsBestGame() {
    var username = uniqueUsername();
    var gameId1 = uniqueGameId();
    var gameId2 = uniqueGameId();

    gameStatsService.recordGame(username, gameId1, false, 600_00);
    gameStatsService.recordGame(username, gameId2, false, 800_00);
    // both games end below the $1000 starting balance: -$400 and -$200
    portfolioStatsService.recordPortfolio(username, gameId1, 600_00, 1000_00, 0);
    portfolioStatsService.recordPortfolio(username, gameId2, 800_00, 1000_00, 0);

    var stats = careerStatsService.getCareerStats(username).orElseThrow();
    assertEquals(2, stats.gamesPlayed());
    assertEquals(-600_00, stats.totalEarningsCents());
    assertEquals(-200_00, stats.bestGameEarningsCents());
  }
}
