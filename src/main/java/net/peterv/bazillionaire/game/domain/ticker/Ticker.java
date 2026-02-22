package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.event.MarketEvent;
import net.peterv.bazillionaire.game.domain.ticker.event.OrderMarketImpactPolicy;
import net.peterv.bazillionaire.game.domain.ticker.strategy.PricingStrategy;
import net.peterv.bazillionaire.game.domain.ticker.strategy.PricingStrategyFactory;
import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.ArrayList;
import java.util.List;

public class Ticker {
	private Money currentPrice;
	private PricingStrategy strategy;
	private final PricingStrategyFactory strategyFactory;
	private final List<MarketEvent> marketEvents; // always mutable

	public Ticker(Money initialPrice, PricingStrategy strategy,
			PricingStrategyFactory strategyFactory, List<MarketEvent> marketEvents) {
		this.currentPrice = initialPrice;
		this.strategy = strategy;
		this.strategyFactory = strategyFactory;
		this.marketEvents = new ArrayList<>(marketEvents);
	}

	public boolean canFill(Order order) {
		return true;
	}

	public Money currentPrice() {
		return currentPrice;
	}

	public void applyOrder(Order order, OrderMarketImpactPolicy policy) {
		this.marketEvents.add(policy.impactOf(order, this));
	}

	public void tick() {
		marketEvents.removeIf(MarketEvent::isExpired);
		marketEvents.replaceAll(MarketEvent::tick);

		if (strategy.isExhausted()) {
			strategy = strategyFactory.nextStrategy();
		}

		MarketForce force = marketEvents.stream()
				.map(e -> e.apply(strategy.kind()))
				.reduce(MarketForce.neutral(), MarketForce::combine);

		currentPrice = strategy.nextPrice(force);
	}
}
