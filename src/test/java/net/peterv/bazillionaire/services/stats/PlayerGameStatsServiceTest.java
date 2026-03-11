package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PlayerGameStatsServiceTest {

  @Inject PlayerGameStatsService service;

  private String uniqueUsername() {
    return "test-" + UUID.randomUUID();
  }

  @Test
  void recordGame_withWin_incrementsWinsAndGamesPlayed() {
    var username = uniqueUsername();
    service.recordGame(username, true);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(1, stats.wins);
    assertEquals(1, stats.gamesPlayed);
  }

  @Test
  void recordGame_withoutWin_incrementsOnlyGamesPlayed() {
    var username = uniqueUsername();
    service.recordGame(username, false);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(0, stats.wins);
    assertEquals(1, stats.gamesPlayed);
  }

  @Test
  void multipleGames_accumulateCorrectly() {
    var username = uniqueUsername();
    service.recordGame(username, true);
    service.recordGame(username, true);
    service.recordGame(username, false);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(2, stats.wins);
    assertEquals(3, stats.gamesPlayed);
  }

  @Test
  void getStats_returnsEmptyForUnknownPlayer() {
    var result = service.getStats("nonexistent-" + UUID.randomUUID());
    assertTrue(result.isEmpty());
  }
}
