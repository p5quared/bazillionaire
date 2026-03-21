package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class PlayerGameStatsService {

  @Transactional
  public void recordGame(String username, String gameId, boolean won, int finalCashCents) {
    var result = new PlayerGameResult();
    result.username = username;
    result.gameId = gameId;
    result.won = won;
    result.finalCashCents = finalCashCents;
    result.playedAt = Instant.now();
    result.persist();
  }

  public Optional<PlayerStatsSummary> getStats(String username) {
    long gamesPlayed = PlayerGameResult.countGames(username);
    if (gamesPlayed == 0) {
      return Optional.empty();
    }
    long wins = PlayerGameResult.countWins(username);
    return Optional.of(new PlayerStatsSummary(username, wins, gamesPlayed));
  }
}
