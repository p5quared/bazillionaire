package net.peterv.bazillionaire.game.domain;

import java.util.HashMap;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class TokenBucketLiquidityLimiter implements LiquidityProvider {
  private final Map<PlayerId, Map<Symbol, Double>> buckets = new HashMap<>();
  private final int maxTokens;
  private final int refillInterval;
  private int lastTick = 0;

  public TokenBucketLiquidityLimiter() {
    this(25, 5);
  }

  public TokenBucketLiquidityLimiter(int maxTokens, int refillInterval) {
    this.maxTokens = maxTokens;
    this.refillInterval = refillInterval;
  }

  @Override
  public boolean canFill(PlayerId playerId, Symbol symbol, int currentTick) {
    double tokens = getBucket(playerId, symbol);
    return tokens >= 1.0;
  }

  @Override
  public void recordFill(PlayerId playerId, Symbol symbol, int currentTick) {
    double tokens = getBucket(playerId, symbol);
    buckets.computeIfAbsent(playerId, k -> new HashMap<>()).put(symbol, tokens - 1.0);
  }

  @Override
  public void onTick(int currentTick) {
    int elapsed = currentTick - lastTick;
    if (elapsed <= 0) {
      lastTick = currentTick;
      return;
    }
    double tokensToAdd = (double) elapsed / refillInterval;
    for (Map<Symbol, Double> symbolMap : buckets.values()) {
      for (Map.Entry<Symbol, Double> entry : symbolMap.entrySet()) {
        double newTokens = Math.min(entry.getValue() + tokensToAdd, maxTokens);
        entry.setValue(newTokens);
      }
    }
    lastTick = currentTick;
  }

  private double getBucket(PlayerId playerId, Symbol symbol) {
    return buckets
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .computeIfAbsent(symbol, k -> (double) maxTokens);
  }
}
