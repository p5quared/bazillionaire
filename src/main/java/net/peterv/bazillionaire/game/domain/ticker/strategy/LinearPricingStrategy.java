package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.ticker.MarketForce;
import net.peterv.bazillionaire.game.domain.types.Money;

public class LinearPricingStrategy implements PricingStrategy {

	private final int stepCents;
	private final int duration;
	private final int direction;

	private int ticks = 0;
	private int currentCents;

	/**
	 * Moves price linearly by direction * stepCents per tick for duration ticks.
	 * 
	 * @param initialPrice starting price
	 * @param stepCents    price change per tick (always positive)
	 * @param duration     number of ticks before exhaustion
	 * @param direction    1 for upward, -1 for downward
	 */
	public LinearPricingStrategy(Money initialPrice, int stepCents, int duration, int direction) {
		this.currentCents = initialPrice.cents();
		this.stepCents = stepCents;
		this.duration = duration;
		this.direction = direction;
	}

	@Override
	public Money nextPrice(MarketForce marketForce) {
		this.ticks++;
		if (isExhausted()) {
			return new Money(currentCents);
		}

		currentCents += direction * stepCents;
		return new Money(currentCents);
	}

	@Override
	public boolean isExhausted() {
		return this.ticks >= duration;
	}

	@Override
	public StrategyKind kind() {
		return StrategyKind.LINEAR;
	}
}
