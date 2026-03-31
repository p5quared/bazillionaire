package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class PlayerPowerupStatsService {

  @Transactional
  public void recordPowerups(
      String username,
      String gameId,
      int powerupsReceived,
      int powerupsUsed,
      int timesFrozen,
      int darkPoolUses) {
    var result = new PlayerPowerupResult();
    result.username = username;
    result.gameId = gameId;
    result.powerupsReceived = powerupsReceived;
    result.powerupsUsed = powerupsUsed;
    result.timesFrozen = timesFrozen;
    result.darkPoolUses = darkPoolUses;
    result.playedAt = Instant.now();
    result.persist();
  }
}
