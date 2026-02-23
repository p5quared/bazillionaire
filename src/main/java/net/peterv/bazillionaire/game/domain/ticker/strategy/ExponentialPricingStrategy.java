package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public class ExponentialPricingStrategy implements PricingStrategy {

	private final int startCents;
	private final int endCents;
	private final int duration;
	private final double curvature;
	private final double expDenom;

	private int ticks = 0;

	/**
	 * Moves price from startPrice to endPrice following an exponential curve.
	 *
	 * @param startPrice starting price
	 * @param endPrice   target price at exhaustion
	 * @param duration   number of ticks to reach endPrice
	 * @param curvature  controls acceleration (1.0 = nearly linear, 5.0 = sharply exponential)
	 */
	public ExponentialPricingStrategy(Money startPrice, Money endPrice, int duration, double curvature) {
		this.startCents = startPrice.cents();
		this.endCents = endPrice.cents();
		this.duration = duration;
		this.curvature = curvature;
		this.expDenom = Math.exp(curvature) - 1.0;
	}

	@Override
	public Money nextPrice() {
		ticks++;
		if (isExhausted()) {
			return new Money(endCents);
		}

		double progress = (double) ticks / duration;
		double normalized = (Math.exp(curvature * progress) - 1.0) / expDenom;
		int currentCents = startCents + (int) (normalized * (endCents - startCents));
		return new Money(currentCents);
	}

	@Override
	public boolean isExhausted() {
		return ticks >= duration;
	}

	@Override
	public StrategyKind kind() {
		return StrategyKind.EXPONENTIAL;
	}
}
