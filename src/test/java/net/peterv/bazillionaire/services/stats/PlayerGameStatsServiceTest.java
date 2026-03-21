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

  private String uniqueGameId() {
    return "game-" + UUID.randomUUID();
  }

  @Test
  void recordGame_withWin_reflectsInStats() {
    var username = uniqueUsername();
    service.recordGame(username, uniqueGameId(), true, 500_00);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(1, stats.wins());
    assertEquals(1, stats.gamesPlayed());
  }

  @Test
  void recordGame_withoutWin_reflectsInStats() {
    var username = uniqueUsername();
    service.recordGame(username, uniqueGameId(), false, 300_00);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(0, stats.wins());
    assertEquals(1, stats.gamesPlayed());
  }

  @Test
  void multipleGames_accumulateCorrectly() {
    var username = uniqueUsername();
    service.recordGame(username, uniqueGameId(), true, 800_00);
    service.recordGame(username, uniqueGameId(), true, 600_00);
    service.recordGame(username, uniqueGameId(), false, 200_00);

    var stats = service.getStats(username).orElseThrow();
    assertEquals(2, stats.wins());
    assertEquals(3, stats.gamesPlayed());
  }

  @Test
  void getStats_returnsEmptyForUnknownPlayer() {
    var result = service.getStats("nonexistent-" + UUID.randomUUID());
    assertTrue(result.isEmpty());
  }

  @Test
  void eachGameCreatesASeparateRecord() {
    var username = uniqueUsername();
    var gameId1 = uniqueGameId();
    var gameId2 = uniqueGameId();
    service.recordGame(username, gameId1, true, 500_00);
    service.recordGame(username, gameId2, false, 300_00);

    var results = PlayerGameResult.findByUsername(username);
    assertEquals(2, results.size());
  }
}
