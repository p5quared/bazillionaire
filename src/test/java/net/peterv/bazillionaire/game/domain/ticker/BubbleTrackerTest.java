package net.peterv.bazillionaire.game.domain.ticker;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BubbleTrackerTest {

  @Test
  void startsInNormalState() {
    var t = new BubbleTracker(10, 5);
    assertEquals(BubbleState.NORMAL, t.state());
    assertEquals(0, t.bubbleFactor());
  }

  @Test
  void recordTradeAccumulatesPoints() {
    var t = new BubbleTracker(10, 5);
    t.recordTrade(1, 1);
    t.recordTrade(1, 1);
    t.recordTrade(2, 3);
    assertEquals(5, t.bubbleFactor());
  }

  @Test
  void transitionsToOverheatedWhenThresholdReached() {
    var t = new BubbleTracker(10, 3);
    t.recordTrade(1, 3);
    t.onTick(1);
    assertEquals(BubbleState.OVERHEATED, t.state());
  }

  @Test
  void staysNormalBelowThreshold() {
    var t = new BubbleTracker(10, 5);
    t.recordTrade(1, 4);
    t.onTick(1);
    assertEquals(BubbleState.NORMAL, t.state());
  }

  @Test
  void returnsToNormalWhenPointsDecay() {
    var t = new BubbleTracker(5, 3);
    t.recordTrade(1, 3);
    t.onTick(1);
    assertEquals(BubbleState.OVERHEATED, t.state());

    t.onTick(7);
    assertEquals(BubbleState.NORMAL, t.state());
  }

  @Test
  void slidingWindowPrunesOldEntries() {
    var t = new BubbleTracker(5, 3);
    t.recordTrade(1, 3);
    assertEquals(3, t.bubbleFactor());

    t.onTick(7);
    assertEquals(0, t.bubbleFactor());
  }

  @Test
  void markDelistedTransitionsToDelisted() {
    var t = new BubbleTracker(10, 3);
    t.recordTrade(1, 5);
    t.onTick(1);
    assertEquals(BubbleState.OVERHEATED, t.state());

    t.markDelisted();
    assertEquals(BubbleState.DELISTED, t.state());
  }

  @Test
  void delistedIsIrreversible() {
    var t = new BubbleTracker(10, 3);
    t.markDelisted();
    t.onTick(100);
    assertEquals(BubbleState.DELISTED, t.state());
  }

  @Test
  void multipleTradesAtSameTickAccumulate() {
    var t = new BubbleTracker(10, 5);
    t.recordTrade(1, 1);
    t.recordTrade(1, 1);
    t.recordTrade(1, 1);
    t.recordTrade(1, 1);
    t.recordTrade(1, 1);
    t.onTick(1);
    assertEquals(BubbleState.OVERHEATED, t.state());
    assertEquals(5, t.bubbleFactor());
  }

  @Test
  void pointsFromDifferentTicksWithinWindowSum() {
    var t = new BubbleTracker(10, 5);
    t.recordTrade(1, 2);
    t.recordTrade(3, 2);
    t.recordTrade(5, 2);
    assertEquals(6, t.bubbleFactor());
    t.onTick(5);
    assertEquals(BubbleState.OVERHEATED, t.state());
  }

  @Test
  void thresholdAccessor() {
    var t = new BubbleTracker(10, 7);
    assertEquals(7, t.threshold());
  }
}
