package net.peterv.bazillionaire.game.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class SlidingWindowLiquidityLimiter implements LiquidityProvider {
  private final Map<PlayerId, Map<Symbol, Deque<Integer>>> fillHistory = new HashMap<>();
  private final int windowSize;
  private final int maxFills;

  public SlidingWindowLiquidityLimiter() {
    this(100, 10);
  }

  public SlidingWindowLiquidityLimiter(int windowSize, int maxFills) {
    this.windowSize = windowSize;
    this.maxFills = maxFills;
  }

  @Override
  public boolean canFill(PlayerId playerId, Symbol symbol, int currentTick) {
    Deque<Integer> history = getHistory(playerId, symbol);
    pruneDeque(history, currentTick);
    return history.size() < maxFills;
  }

  @Override
  public void recordFill(PlayerId playerId, Symbol symbol, int currentTick) {
    getHistory(playerId, symbol).addLast(currentTick);
  }

  @Override
  public void onTick(int currentTick) {
    fillHistory
        .values()
        .forEach(symbolMap -> symbolMap.values().forEach(deque -> pruneDeque(deque, currentTick)));
  }

  private Deque<Integer> getHistory(PlayerId playerId, Symbol symbol) {
    return fillHistory
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .computeIfAbsent(symbol, k -> new ArrayDeque<>());
  }

  private void pruneDeque(Deque<Integer> deque, int currentTick) {
    int cutoff = currentTick - windowSize;
    while (!deque.isEmpty() && deque.peekFirst() <= cutoff) {
      deque.pollFirst();
    }
  }
}
