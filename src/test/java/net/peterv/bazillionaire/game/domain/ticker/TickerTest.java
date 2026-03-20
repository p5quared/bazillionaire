package net.peterv.bazillionaire.game.domain.ticker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import net.peterv.bazillionaire.game.domain.ticker.regime.DefaultRegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

class TickerTest {

  private static final Money INITIAL_PRICE = new Money(100_00);
  private static final int TOTAL_DURATION = 200;
  private static final long SEED = 42L;

  private Ticker createTicker() {
    return new Ticker(new DefaultRegimeFactory(new Random(SEED)), INITIAL_PRICE);
  }

  @Test
  void strategyLinkageNoPriceDiscontinuities() {
    var ticker = createTicker();
    ticker.tick();
    Money prev = ticker.currentPrice();

    for (int i = 1; i < TOTAL_DURATION; i++) {
      ticker.tick();
      Money current = ticker.currentPrice();
      int jump = Math.abs(current.cents() - prev.cents());
      assertTrue(
          jump < prev.cents(),
          "Discontinuity at tick %d: %d -> %d (jump=%d)"
              .formatted(i, prev.cents(), current.cents(), jump));
      prev = current;
    }
  }

  @Test
  void currentPriceBeforeFirstTickReturnsInitialPrice() {
    var ticker = createTicker();
    assertEquals(INITIAL_PRICE, ticker.currentPrice());
  }

  @Test
  void allPricesArePositive() {
    var ticker = createTicker();
    for (int i = 0; i < TOTAL_DURATION; i++) {
      ticker.tick();
      assertTrue(
          ticker.currentPrice().cents() > 0, "Price at tick %d should be positive".formatted(i));
    }
  }

  @Test
  void tickBeyondDurationDoesNotThrow() {
    var ticker = createTicker();
    for (int i = 0; i < TOTAL_DURATION * 3; i++) {
      ticker.tick();
    }
    assertDoesNotThrow(ticker::currentPrice);
  }

  @Test
  void keepsGeneratingPricesAfterInitialWindow() {
    var ticker = createTicker();

    for (int i = 0; i < TOTAL_DURATION * 3; i++) {
      ticker.tick();
    }

    assertTrue(ticker.currentPrice().cents() > 0);
  }
}
