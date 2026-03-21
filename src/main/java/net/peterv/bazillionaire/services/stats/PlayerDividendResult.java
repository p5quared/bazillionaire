package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;

@Entity
public class PlayerDividendResult extends PanacheEntity {

  @Column(nullable = false)
  public String username;

  @Column(nullable = false)
  public String gameId;

  @Column(nullable = false)
  public int dividendsCollected;

  @Column(nullable = false)
  public int dividendCashCents;

  @Column(nullable = false)
  public Instant playedAt;

  public static List<PlayerDividendResult> findByUsername(String username) {
    return list("username", username);
  }
}
