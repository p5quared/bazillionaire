package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;

@Entity
public class PlayerGameResult extends PanacheEntity {

  @Column(nullable = false)
  public String username;

  @Column(nullable = false)
  public String gameId;

  public boolean won;

  public int finalCashCents;

  @Column(nullable = false)
  public Instant playedAt;

  public static List<PlayerGameResult> findByUsername(String username) {
    return list("username", username);
  }

  public static List<PlayerGameResult> findByGameId(String gameId) {
    return list("gameId", gameId);
  }

  public static long countGames(String username) {
    return count("username", username);
  }

  public static long countWins(String username) {
    return count("username = ?1 and won = true", username);
  }
}
