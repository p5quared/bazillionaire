package net.peterv.bazillionaire.game.domain.ticker;

import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.ticker.regime.DefaultRegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class TickerBubbleAccessorTest {

  private static final Symbol AAPL = new Symbol("AAPL");

  @Test
  void bubbleFactorReflectsRecordedTrades() {
    var tracker = new BubbleTracker(20, 25);
    var ticker =
        new Ticker(
            new DefaultRegimeFactory(new java.util.Random(42), MarketCap.STARTUP),
            new Money(10000),
            MarketCap.STARTUP,
            tracker,
            new java.util.Random(42));

    assertEquals(0, ticker.bubbleFactor());
    ticker.recordTrade(1, 3);
    assertEquals(3, ticker.bubbleFactor());
  }

  @Test
  void bubbleThresholdMatchesTracker() {
    var tracker = new BubbleTracker(20, 25);
    var ticker =
        new Ticker(
            new DefaultRegimeFactory(new java.util.Random(42), MarketCap.STARTUP),
            new Money(10000),
            MarketCap.STARTUP,
            tracker,
            new java.util.Random(42));

    assertEquals(25, ticker.bubbleThreshold());
  }
}
