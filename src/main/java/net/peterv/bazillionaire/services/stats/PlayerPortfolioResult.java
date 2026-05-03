package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;

@Entity
public class PlayerPortfolioResult extends PanacheEntity {

  @Column(nullable = false)
  public String username;

  @Column(nullable = false)
  public String gameId;

  @Column(nullable = false)
  public long finalPortfolioValueCents;

  @Column(nullable = false)
  public long startingBalanceCents;

  @Column(nullable = false)
  public int holdingsCount;

  @Column(nullable = false)
  public Instant playedAt;

  public static List<PlayerPortfolioResult> findByUsername(String username) {
    return list("username", username);
  }

  public static List<PlayerPortfolioResult> findByGameId(String gameId) {
    return list("gameId", gameId);
  }
}
