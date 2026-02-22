package net.peterv.bazillionaire.game.domain.ticker.event;

import net.peterv.bazillionaire.game.domain.ticker.MarketForce;
import net.peterv.bazillionaire.game.domain.ticker.strategy.StrategyKind;

public interface MarketEvent {
	MarketForce apply(StrategyKind activeStrategy);

	boolean isExpired();

	MarketEvent tick();
}
