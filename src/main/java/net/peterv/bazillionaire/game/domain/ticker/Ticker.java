package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.strategy.PricingStrategy;
import net.peterv.bazillionaire.game.domain.ticker.strategy.PricingStrategyFactory;
import net.peterv.bazillionaire.game.domain.types.Money;

public class Ticker {
	private Money currentPrice;
	private PricingStrategy strategy;
	private final PricingStrategyFactory strategyFactory;

	public Ticker(Money initialPrice, PricingStrategy strategy,
			PricingStrategyFactory strategyFactory) {
		this.currentPrice = initialPrice;
		this.strategy = strategy;
		this.strategyFactory = strategyFactory;
	}

	public boolean canFill(Order order) {
		return switch (order) {
			case Order.Buy o -> o.price().isGreaterThanOrEqualTo(this.currentPrice());
			case Order.Sell o -> this.currentPrice().isGreaterThanOrEqualTo(o.price());
		};
	}

	public Money currentPrice() {
		return currentPrice;
	}

	public void tick() {
		if (strategy.isExhausted()) {
			strategy = strategyFactory.nextStrategy();
		}

		currentPrice = strategy.nextPrice();
	}
}
