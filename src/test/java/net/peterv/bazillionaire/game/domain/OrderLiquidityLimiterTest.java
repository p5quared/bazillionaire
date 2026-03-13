package net.peterv.bazillionaire.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class OrderLiquidityLimiterTest {

  private static final PlayerId PLAYER_1 = new PlayerId("player1");
  private static final PlayerId PLAYER_2 = new PlayerId("player2");
  private static final Symbol AAPL = new Symbol("AAPL");
  private static final Symbol GOOG = new Symbol("GOOG");

  @Test
  void allowsFillsWithinCap() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.canFill(PLAYER_1, AAPL, 0));
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
  }

  @Test
  void rejectsWhenCapReached() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
  }

  @Test
  void differentTickersHaveIndependentCaps() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
    assertTrue(limiter.canFill(PLAYER_1, GOOG, 0));
  }

  @Test
  void differentPlayersHaveIndependentCaps() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
    assertTrue(limiter.canFill(PLAYER_2, AAPL, 0));
  }

  @Test
  void windowSlides_oldFillsExpire() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 50));
    assertTrue(limiter.canFill(PLAYER_1, AAPL, 100));
  }

  @Test
  void pruneRemovesStaleEntries() {
    var limiter = new OrderLiquidityLimiter(100, 10);
    for (int i = 0; i < 10; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 50));
    limiter.onTick(100);
    assertTrue(limiter.canFill(PLAYER_1, AAPL, 100));
  }
}
