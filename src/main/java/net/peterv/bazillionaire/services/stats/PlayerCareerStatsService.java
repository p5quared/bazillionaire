package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class PlayerCareerStatsService {

  public Optional<PlayerCareerStats> getCareerStats(String username) {
    long gamesPlayed = PlayerGameResult.countGames(username);
    if (gamesPlayed == 0) {
      return Optional.empty();
    }
    long wins = PlayerGameResult.countWins(username);
    double winRate = (double) wins / gamesPlayed;

    var portfolioResults = PlayerPortfolioResult.findByUsername(username);
    long totalEarningsCents = 0;
    long bestGameValueCents = 0;
    for (var pr : portfolioResults) {
      totalEarningsCents += pr.finalPortfolioValueCents;
      if (pr.finalPortfolioValueCents > bestGameValueCents) {
        bestGameValueCents = pr.finalPortfolioValueCents;
      }
    }

    var tradeResults = PlayerTradeResult.findByUsername(username);
    long totalTradesMade = 0;
    long totalOrdersBlocked = 0;
    for (var tr : tradeResults) {
      totalTradesMade += tr.totalBuys + tr.totalSells;
      totalOrdersBlocked += tr.totalBlockedOrders;
    }

    var powerupResults = PlayerPowerupResult.findByUsername(username);
    long totalPowerupsReceived = 0;
    long totalPowerupsUsed = 0;
    long timesFrozen = 0;
    long darkPoolUses = 0;
    for (var pu : powerupResults) {
      totalPowerupsReceived += pu.powerupsReceived;
      totalPowerupsUsed += pu.powerupsUsed;
      timesFrozen += pu.timesFrozen;
      darkPoolUses += pu.darkPoolUses;
    }

    var dividendResults = PlayerDividendResult.findByUsername(username);
    long totalDividendsCollected = 0;
    long totalDividendCashCents = 0;
    for (var dr : dividendResults) {
      totalDividendsCollected += dr.dividendsCollected;
      totalDividendCashCents += dr.dividendCashCents;
    }

    return Optional.of(
        new PlayerCareerStats(
            username,
            gamesPlayed,
            wins,
            winRate,
            totalEarningsCents,
            bestGameValueCents,
            totalTradesMade,
            totalOrdersBlocked,
            totalPowerupsReceived,
            totalPowerupsUsed,
            totalDividendsCollected,
            totalDividendCashCents,
            timesFrozen,
            darkPoolUses));
  }
}
