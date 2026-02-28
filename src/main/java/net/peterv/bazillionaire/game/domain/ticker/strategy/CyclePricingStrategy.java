package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public class CyclePricingStrategy implements PricingStrategy {

	private final int amplitudeCents;
	private final int initialCents;
	private final int direction;
	private final int waveLength;

	/**
	 * Determinstically cycles price using a sine wave.
	 * 
	 * @param initialPrice   the center price the wave oscillates around
	 * @param amplitudeCents peak deviation from initialPrice in cents
	 * @param waveLength     number of ticks for one full cycle
	 * @param direction      1 to start moving up, -1 to start moving down
	 */
	public CyclePricingStrategy(Money initialPrice, int amplitudeCents, int waveLength, int direction) {
		this.initialCents = initialPrice.cents();
		this.amplitudeCents = amplitudeCents;
		this.waveLength = waveLength;
		this.direction = direction;
	}

	@Override
	public Money priceAt(int tick) {
		int effectiveTick = Math.min(tick + 1, waveLength - 1);
		double phase = 2 * Math.PI * effectiveTick / waveLength + (direction == -1 ? Math.PI : 0);
		return new Money(initialCents + (int) (amplitudeCents * Math.sin(phase)));
	}
}
