package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;

@Entity
public class PlayerPowerupResult extends PanacheEntity {

  @Column(nullable = false)
  public String username;

  @Column(nullable = false)
  public String gameId;

  @Column(nullable = false)
  public int powerupsReceived;

  @Column(nullable = false)
  public int powerupsUsed;

  @Column(nullable = false)
  public int timesFrozen;

  @Column(nullable = false)
  public int darkPoolUses;

  @Column(nullable = false)
  public Instant playedAt;

  public static List<PlayerPowerupResult> findByUsername(String username) {
    return list("username", username);
  }

  public static List<PlayerPowerupResult> findByGameId(String gameId) {
    return list("gameId", gameId);
  }
}
