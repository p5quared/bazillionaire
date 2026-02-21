package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.ticker.MarketForce;
import net.peterv.bazillionaire.game.domain.types.Money;

public interface PricingStrategy {
    Money nextPrice(MarketForce marketForce);
    boolean isExhausted();
    StrategyKind kind();
}
