package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public class LinearPricingStrategy implements PricingStrategy {

	private final int initialCents;
	private final int stepCents;
	private final int duration;
	private final int direction;

	/**
	 * Moves price linearly by direction * stepCents per tick for duration ticks.
	 *
	 * @param initialPrice starting price
	 * @param stepCents    price change per tick (always positive)
	 * @param duration     number of ticks before exhaustion
	 * @param direction    1 for upward, -1 for downward
	 */
	public LinearPricingStrategy(Money initialPrice, int stepCents, int duration, int direction) {
		this.initialCents = initialPrice.cents();
		this.stepCents = stepCents;
		this.duration = duration;
		this.direction = direction;
	}

	@Override
	public Money priceAt(int tick) {
		int steps = Math.min(tick + 1, duration - 1);
		return new Money(initialCents + steps * direction * stepCents);
	}
}
