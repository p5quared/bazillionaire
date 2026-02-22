package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.ticker.MarketForce;
import net.peterv.bazillionaire.game.domain.types.Money;

public class CyclePricingStrategy implements PricingStrategy {

	private final int stepCents;
	private int currentCents;
	private int direction;
	private final int waveLength;

	private int ticks = 0;

	/**
	 * Determinstically cycles price up and down or down and up.
	 * @param initialPrice
	 * @param stepCents
	 * @param waveLength
	 * @param direction
	 */
	public CyclePricingStrategy(Money initialPrice, int stepCents, int waveLength, int direction) {
		this.currentCents = initialPrice.cents();
		this.stepCents = stepCents;
		this.waveLength = waveLength;
		this.direction = direction;
	}

	@Override
	public Money nextPrice(MarketForce marketForce) {
		this.ticks++;
		if (isExhausted()) {
			return new Money(currentCents);
		}

		currentCents += direction * stepCents;
		if (ticks == waveLength / 2) {
			this.direction *= -1;
		}
		return new Money(currentCents);
	}

	@Override
	public boolean isExhausted() {
		return this.ticks >= waveLength;
	}

	@Override
	public StrategyKind kind() {
		return StrategyKind.CYCLE;
	}
}
