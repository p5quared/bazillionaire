package net.peterv.bazillionaire.game.domain.ticker.strategy;

import java.util.List;

public class CyclicPricingStrategyFactory implements PricingStrategyFactory {
	private final List<PricingStrategy> strategies;
	private int currentStrategyIdx = -1;

	public CyclicPricingStrategyFactory(List<PricingStrategy> strategies) {
		this.strategies = strategies;
	}

	@Override
	public PricingStrategy nextStrategy() {
		currentStrategyIdx++;
		currentStrategyIdx %= strategies.size();
		return strategies.get(currentStrategyIdx);
	}
}
