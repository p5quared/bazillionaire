package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class PlayerTradeStatsService {

  @Transactional
  public void recordTrades(
      String username,
      String gameId,
      int totalBuys,
      int totalSells,
      long totalFillsCents,
      int totalBlockedOrders) {
    var result = new PlayerTradeResult();
    result.username = username;
    result.gameId = gameId;
    result.totalBuys = totalBuys;
    result.totalSells = totalSells;
    result.totalFillsCents = totalFillsCents;
    result.totalBlockedOrders = totalBlockedOrders;
    result.playedAt = Instant.now();
    result.persist();
  }
}
