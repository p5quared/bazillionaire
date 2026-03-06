package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.ArrayList;
import java.util.List;

public class LogisticRegimeStrategy implements RegimeStrategy {

	private final int startCents;
	private final int endCents;
	private final int duration;
	private final double steepness;
	private final double logisticAt0;
	private final double logisticRange;

	/**
	 * Moves price from startPrice to endPrice following a logistic S-curve.
	 *
	 * @param startPrice starting price
	 * @param endPrice   target price at exhaustion
	 * @param duration   number of ticks to reach endPrice
	 * @param steepness  controls S-curve sharpness (e.g. 6.0 = moderate, 12.0 =
	 *                   sharp)
	 */
	public LogisticRegimeStrategy(Money startPrice, Money endPrice, int duration, double steepness) {
		this.startCents = startPrice.cents();
		this.endCents = endPrice.cents();
		this.duration = duration;
		this.steepness = steepness;

		this.logisticAt0 = rawLogistic(0.0);
		double logisticAt1 = rawLogistic(1.0);
		this.logisticRange = logisticAt1 - logisticAt0;
	}

	private double rawLogistic(double progress) {
		return 1.0 / (1.0 + Math.exp(-steepness * (progress - 0.5)));
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
		if (tick >= duration - 1)
			return new Money(endCents);
		double progress = (double) tick / duration;
		double normalized = (rawLogistic(progress) - logisticAt0) / logisticRange;
		return new Money(startCents + (int) (normalized * (endCents - startCents)));
	}
}
