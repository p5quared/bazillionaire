package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.List;
import java.util.Random;

public class RegimeFactory {

	private static final RegimeKind[] KINDS = RegimeKind.values();

	private final int regimeDuration;
	private final Random random;
	private Money nextStartPrice;

	public RegimeFactory(Money initialPrice, int regimeDuration, Random random) {
		if (regimeDuration <= 0) {
			throw new IllegalArgumentException("Regime duration must be positive");
		}
		this.nextStartPrice = initialPrice;
		this.regimeDuration = regimeDuration;
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
		return switch (kind) {
			case LINEAR -> {
				Money endPrice = randomEndPrice(startPrice);
				int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
				int stepCents = Math.max(1, deltaCents / regimeDuration);
				int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
				yield new LinearRegimeStrategy(startPrice, stepCents, regimeDuration, direction);
			}
			case EXPONENTIAL -> {
				Money endPrice = randomEndPrice(startPrice);
				double curvature = 1.0 + random.nextDouble() * 4.0;
				yield new ExponentialRegimeStrategy(startPrice, endPrice, regimeDuration, curvature);
			}
			case LOGISTIC -> {
				Money endPrice = randomEndPrice(startPrice);
				double steepness = 4.0 + random.nextDouble() * 8.0;
				yield new LogisticRegimeStrategy(startPrice, endPrice, regimeDuration, steepness);
			}
			case CYCLE -> {
				int amplitudeCents = Math.max(1, (int) (startPrice.cents() * (0.1 + random.nextDouble() * 0.3)));
				int direction = random.nextBoolean() ? 1 : -1;
				yield new CycleRegimeStrategy(startPrice, amplitudeCents, regimeDuration, direction);
			}
		};
	}

	private Money randomEndPrice(Money startPrice) {
		double factor = 0.5 + random.nextDouble();
		int endCents = Math.max(1, (int) (startPrice.cents() * factor));
		return new Money(endCents);
	}
}
