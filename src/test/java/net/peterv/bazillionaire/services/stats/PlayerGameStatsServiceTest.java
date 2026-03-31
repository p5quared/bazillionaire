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
