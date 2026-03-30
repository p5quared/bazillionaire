package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public interface LiquidityProvider {
  boolean canFill(PlayerId playerId, Symbol symbol, int currentTick);

  void recordFill(PlayerId playerId, Symbol symbol, int currentTick);

  void onTick(int currentTick);

  int remainingTokens(PlayerId playerId, Symbol symbol);

  int maxTokens();
}
