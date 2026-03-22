package net.peterv.bazillionaire.game.domain.ticker;

import java.util.ArrayDeque;
import java.util.Deque;

public class BubbleTracker {
  private final int windowSize;
  private final int bubbleThreshold;
  private final Deque<int[]> entries = new ArrayDeque<>();
  private BubbleState state = BubbleState.NORMAL;

  public BubbleTracker(int windowSize, int bubbleThreshold) {
    this.windowSize = windowSize;
    this.bubbleThreshold = bubbleThreshold;
  }

  public void recordTrade(int currentTick, int points) {
    entries.addLast(new int[] {currentTick, points});
  }

  public void onTick(int currentTick) {
    if (state == BubbleState.DELISTED) {
      return;
    }

    pruneExpired(currentTick);

    if (bubbleFactor() >= bubbleThreshold) {
      if (state == BubbleState.NORMAL) {
        state = BubbleState.OVERHEATED;
      }
    } else {
      if (state == BubbleState.OVERHEATED) {
        state = BubbleState.NORMAL;
      }
    }
  }

  public int bubbleFactor() {
    int sum = 0;
    for (int[] entry : entries) {
      sum += entry[1];
    }
    return sum;
  }

  public BubbleState state() {
    return state;
  }

  public int threshold() {
    return bubbleThreshold;
  }

  public void markDelisted() {
    state = BubbleState.DELISTED;
  }

  private void pruneExpired(int currentTick) {
    int cutoff = currentTick - windowSize;
    while (!entries.isEmpty() && entries.peekFirst()[0] <= cutoff) {
      entries.pollFirst();
    }
  }
}
