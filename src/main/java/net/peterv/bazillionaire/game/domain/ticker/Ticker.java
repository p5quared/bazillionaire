package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.regime.*;
import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ticker {
	private final Money initialPrice;
	private final List<Money> prices;
	private int cursor = -1;

	public Ticker(Money initialPrice, int totalDuration, int regimeDuration, Random random) {
		this.initialPrice = initialPrice;
		this.prices = buildPriceTimeline(initialPrice, totalDuration, regimeDuration, random);
	}

	public boolean canFill(Order order) {
		return switch (order) {
			case Order.Buy o -> o.price().isGreaterThanOrEqualTo(this.currentPrice());
			case Order.Sell o -> this.currentPrice().isGreaterThanOrEqualTo(o.price());
		};
	}

	public Money currentPrice() {
		if (cursor < 0) {
			return initialPrice;
		}
		return prices.get(cursor);
	}

	public void tick() {
		if (cursor < prices.size() - 1) {
			cursor++;
		}
	}

	private static List<Money> buildPriceTimeline(Money initialPrice, int totalDuration,
			int regimeDuration, Random random) {
		RegimeKind[] kinds = RegimeKind.values();
		int segmentCount = totalDuration / regimeDuration;
		List<Money> timeline = new ArrayList<>(totalDuration);
		Money segmentStart = initialPrice;

		for (int seg = 0; seg < segmentCount; seg++) {
			RegimeKind kind = kinds[random.nextInt(kinds.length)];
			RegimeStrategy regime = createRegime(kind, segmentStart, regimeDuration, random);
			List<Money> regimePrices = regime.prices();
			timeline.addAll(regimePrices);
			segmentStart = regimePrices.get(regimePrices.size() - 1);
		}

		return List.copyOf(timeline);
	}

	private static RegimeStrategy createRegime(RegimeKind kind, Money startPrice,
			int duration, Random random) {
		return switch (kind) {
			case LINEAR -> {
				Money endPrice = randomEndPrice(startPrice, random);
				int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
				int stepCents = Math.max(1, deltaCents / duration);
				int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
				yield new LinearRegimeStrategy(startPrice, stepCents, duration, direction);
			}
			case EXPONENTIAL -> {
				Money endPrice = randomEndPrice(startPrice, random);
				double curvature = 1.0 + random.nextDouble() * 4.0;
				yield new ExponentialRegimeStrategy(startPrice, endPrice, duration, curvature);
			}
			case LOGISTIC -> {
				Money endPrice = randomEndPrice(startPrice, random);
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

	private static Money randomEndPrice(Money startPrice, Random random) {
		double factor = 0.5 + random.nextDouble();
		int endCents = Math.max(1, (int) (startPrice.cents() * factor));
		return new Money(endCents);
	}
}
