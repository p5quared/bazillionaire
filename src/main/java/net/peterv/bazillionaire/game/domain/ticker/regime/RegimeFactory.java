package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.List;
import java.util.Random;

public class RegimeFactory {

	private static final RegimeKind[] KINDS = RegimeKind.values();

	private final int minRegimeDuration;
	private final int maxRegimeDuration;
	private final Random random;
	private Money nextStartPrice;

	public RegimeFactory(Money initialPrice, int minRegimeDuration, int maxRegimeDuration, Random random) {
		if (minRegimeDuration < 1) {
			throw new IllegalArgumentException("Min regime duration must be >= 1");
		}
		if (maxRegimeDuration < minRegimeDuration) {
			throw new IllegalArgumentException("Max regime duration must be >= min regime duration");
		}
		this.nextStartPrice = initialPrice;
		this.minRegimeDuration = minRegimeDuration;
		this.maxRegimeDuration = maxRegimeDuration;
		this.random = random;
	}

	public RegimeStrategy nextRegime() {
		RegimeKind kind = KINDS[random.nextInt(KINDS.length)];
		RegimeStrategy regime = createRegime(kind, nextStartPrice);
		List<Money> prices = regime.prices();
		nextStartPrice = prices.get(prices.size() - 1);
		return regime;
	}

	private RegimeStrategy createRegime(RegimeKind kind, Money startPrice) {
		int duration = minRegimeDuration + random.nextInt(maxRegimeDuration - minRegimeDuration + 1);
		return switch (kind) {
			case LINEAR -> {
				Money endPrice = randomEndPrice(startPrice);
				int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
				int stepCents = Math.max(1, deltaCents / duration);
				int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
				yield new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
			}
			case EXPONENTIAL -> {
				Money endPrice = randomEndPrice(startPrice);
				double curvature = 1.0 + random.nextDouble() * 4.0;
				yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
			}
			case LOGISTIC -> {
				Money endPrice = randomEndPrice(startPrice);
				double steepness = 4.0 + random.nextDouble() * 8.0;
				yield new LogisticRegimeStrategy(startPrice, endPrice, duration, steepness);
			}
			case CYCLE -> {
				int amplitudeCents = Math.max(1, (int) (startPrice.cents() * (0.1 + random.nextDouble() * 0.3)));
				int direction = random.nextBoolean() ? 1 : -1;
				yield new CycleRegimeStrategy(startPrice, amplitudeCents, duration, direction);
			}
		};
	}

	private Money randomEndPrice(Money startPrice) {
		double factor = 0.5 + random.nextDouble();
		int endCents = Math.max(1, (int) (startPrice.cents() * factor));
		return new Money(endCents);
	}
}
