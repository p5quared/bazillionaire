package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class PlayerPortfolioStatsService {

  @Transactional
  public void recordPortfolio(
      String username, String gameId, long finalPortfolioValueCents, int holdingsCount) {
    var result = new PlayerPortfolioResult();
    result.username = username;
    result.gameId = gameId;
    result.finalPortfolioValueCents = finalPortfolioValueCents;
    result.holdingsCount = holdingsCount;
    result.playedAt = Instant.now();
    result.persist();
  }
}
