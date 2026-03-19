package net.peterv.bazillionaire.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class TokenBucketLiquidityLimiterTest {

  private static final PlayerId PLAYER_1 = new PlayerId("player1");
  private static final PlayerId PLAYER_2 = new PlayerId("player2");
  private static final Symbol AAPL = new Symbol("AAPL");
  private static final Symbol GOOG = new Symbol("GOOG");

  @Test
  void allowsFillsUpToBucketCapacity() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    for (int i = 0; i < 25; i++) {
      assertTrue(limiter.canFill(PLAYER_1, AAPL, 0), "Fill #" + i + " should be allowed");
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
  }

  @Test
  void rejectsWhenBucketEmpty() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    for (int i = 0; i < 25; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
  }

  @Test
  void differentTickersHaveIndependentBuckets() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    for (int i = 0; i < 25; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
    assertTrue(limiter.canFill(PLAYER_1, GOOG, 0));
  }

  @Test
  void differentPlayersHaveIndependentBuckets() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    for (int i = 0; i < 25; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));
    assertTrue(limiter.canFill(PLAYER_2, AAPL, 0));
  }

  @Test
  void tokensRefillViaOnTick() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    for (int i = 0; i < 25; i++) {
      limiter.recordFill(PLAYER_1, AAPL, 0);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 0));

    limiter.onTick(5);
    assertTrue(limiter.canFill(PLAYER_1, AAPL, 5), "Should have 1 token after 5 ticks");
    limiter.recordFill(PLAYER_1, AAPL, 5);
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 5), "Should be empty again after using the token");
  }

  @Test
  void bucketCapsAtMax() {
    var limiter = new TokenBucketLiquidityLimiter(25, 5);
    limiter.onTick(1000);
    for (int i = 0; i < 25; i++) {
      assertTrue(limiter.canFill(PLAYER_1, AAPL, 1000), "Fill #" + i + " should be allowed");
      limiter.recordFill(PLAYER_1, AAPL, 1000);
    }
    assertFalse(limiter.canFill(PLAYER_1, AAPL, 1000), "Should not exceed max tokens");
  }
}
