package net.peterv.bazillionaire.services.stats;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.util.Optional;

@Entity
public class PlayerGameStats extends PanacheEntity {

  @Column(nullable = false, unique = true)
  public String username;

  public int wins;

  public int gamesPlayed;

  public static Optional<PlayerGameStats> findByUsername(String username) {
    return find("username", username).firstResultOptional();
  }
}
