package net.peterv.bazillionaire.game.domain.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GameIdTest {

  @Test
  void validGameIdConstructs() {
    assertEquals("game-1", new GameId("game-1").value());
  }

  @Test
  void nullThrows() {
    assertThrows(IllegalArgumentException.class, () -> new GameId(null));
  }

  @Test
  void blankThrows() {
    assertThrows(IllegalArgumentException.class, () -> new GameId(""));
    assertThrows(IllegalArgumentException.class, () -> new GameId("   "));
  }
}
