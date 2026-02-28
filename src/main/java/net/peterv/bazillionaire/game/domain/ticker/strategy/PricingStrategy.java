package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public interface PricingStrategy {
	Money priceAt(int tick);
}
