package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public class CyclePricingStrategy implements PricingStrategy {

	private final int amplitudeCents;
	private final int initialCents;
	private int currentCents;
	private final int direction;
	private final int waveLength;

	private int ticks = 0;

	/**
	 * Determinstically cycles price using a sine wave.
	 * @param initialPrice the center price the wave oscillates around
	 * @param amplitudeCents peak deviation from initialPrice in cents
	 * @param waveLength number of ticks for one full cycle
	 * @param direction 1 to start moving up, -1 to start moving down
	 */
	public CyclePricingStrategy(Money initialPrice, int amplitudeCents, int waveLength, int direction) {
		this.initialCents = initialPrice.cents();
		this.currentCents = initialPrice.cents();
		this.amplitudeCents = amplitudeCents;
		this.waveLength = waveLength;
		this.direction = direction;
	}

	@Override
	public Money nextPrice() {
		this.ticks++;
		if (isExhausted()) {
			return new Money(currentCents);
		}

		double phase = 2 * Math.PI * ticks / waveLength + (direction == -1 ? Math.PI : 0);
		currentCents = initialCents + (int) (amplitudeCents * Math.sin(phase));
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
