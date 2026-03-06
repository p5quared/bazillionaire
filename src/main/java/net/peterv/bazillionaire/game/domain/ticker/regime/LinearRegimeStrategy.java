package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.ArrayList;
import java.util.List;

public class LinearRegimeStrategy implements RegimeStrategy {

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
	public LinearRegimeStrategy(Money initialPrice, int stepCents, int duration, int direction) {
		this.initialCents = initialPrice.cents();
		this.stepCents = stepCents;
		this.duration = duration;
		this.direction = direction;
	}

	@Override
	public List<Money> prices() {
		List<Money> prices = new ArrayList<>(duration);
		for (int tick = 0; tick < duration; tick++) {
			prices.add(priceAt(tick));
		}
		return List.copyOf(prices);
	}

	private Money priceAt(int tick) {
		return new Money(initialCents + tick * direction * stepCents);
	}
}
