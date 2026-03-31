package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;

@Entity
public class PlayerTradeResult extends PanacheEntity {

  @Column(nullable = false)
  public String username;

  @Column(nullable = false)
  public String gameId;

  @Column(nullable = false)
  public int totalBuys;

  @Column(nullable = false)
  public int totalSells;

  @Column(nullable = false)
  public long totalFillsCents;

  @Column(nullable = false)
  public int totalBlockedOrders;

  @Column(nullable = false)
  public Instant playedAt;

  public static List<PlayerTradeResult> findByUsername(String username) {
    return list("username", username);
  }

  public static List<PlayerTradeResult> findByGameId(String gameId) {
    return list("gameId", gameId);
  }
}
