package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class PlayerGameStatsService {

  @Transactional
  public void recordGame(String username, boolean won) {
    var stats = findOrCreate(username);
    if (won) stats.wins++;
    stats.gamesPlayed++;
  }

  @Transactional
  public Optional<PlayerGameStats> getStats(String username) {
    return PlayerGameStats.findByUsername(username);
  }

  private PlayerGameStats findOrCreate(String username) {
    return PlayerGameStats.findByUsername(username)
        .orElseGet(
            () -> {
              var stats = new PlayerGameStats();
              stats.username = username;
              stats.persist();
              return stats;
            });
  }
}
