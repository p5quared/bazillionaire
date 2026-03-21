package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class PlayerDividendStatsService {

  @Transactional
  public void recordDividends(String username, String gameId, int count, int totalCents) {
    var result = new PlayerDividendResult();
    result.username = username;
    result.gameId = gameId;
    result.dividendsCollected = count;
    result.dividendCashCents = totalCents;
    result.playedAt = Instant.now();
    result.persist();
  }
}
