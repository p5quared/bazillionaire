package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.strategy.*;
import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ticker {
	private final Money initialPrice;
	private final List<Money> prices;
	private int cursor = -1;

	public Ticker(Money initialPrice, int totalDuration, int strategyDuration, Random random) {
		this.initialPrice = initialPrice;
		this.prices = buildPriceTimeline(initialPrice, totalDuration, strategyDuration, random);
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
		cursor++;
	}

	public List<Money> peek(int n) {
		int from = cursor + 1;
		int to = Math.min(from + n, prices.size());
		if (from >= prices.size()) {
			return List.of();
		}
		return List.copyOf(prices.subList(from, to));
	}

	private static List<Money> buildPriceTimeline(Money initialPrice, int totalDuration,
			int strategyDuration, Random random) {
		StrategyKind[] kinds = StrategyKind.values();
		int segmentCount = totalDuration / strategyDuration;
		List<Money> timeline = new ArrayList<>(totalDuration);
		Money segmentStart = initialPrice;

		for (int seg = 0; seg < segmentCount; seg++) {
			StrategyKind kind = kinds[random.nextInt(kinds.length)];
			PricingStrategy strategy = createStrategy(kind, segmentStart, strategyDuration, random);

			Money lastPrice = segmentStart;
			for (int t = 0; t < strategyDuration; t++) {
				lastPrice = strategy.nextPrice();
				timeline.add(lastPrice);
			}
			segmentStart = lastPrice;
		}

		return List.copyOf(timeline);
	}

	private static PricingStrategy createStrategy(StrategyKind kind, Money startPrice,
			int duration, Random random) {
		return switch (kind) {
			case LINEAR -> {
				Money endPrice = randomEndPrice(startPrice, random);
				int deltaCents = Math.abs(endPrice.cents() - startPrice.cents());
				int stepCents = Math.max(1, deltaCents / duration);
				int direction = endPrice.cents() >= startPrice.cents() ? 1 : -1;
				yield new LinearPricingStrategy(startPrice, stepCents, duration, direction);
			}
			case EXPONENTIAL -> {
				Money endPrice = randomEndPrice(startPrice, random);
				double curvature = 1.0 + random.nextDouble() * 4.0;
				yield new ExponentialPricingStrategy(startPrice, endPrice, duration, curvature);
			}
			case LOGISTIC -> {
				Money endPrice = randomEndPrice(startPrice, random);
				double steepness = 4.0 + random.nextDouble() * 8.0;
				yield new LogisticPricingStrategy(startPrice, endPrice, duration, steepness);
			}
			case CYCLE -> {
				int amplitudeCents = Math.max(1, (int) (startPrice.cents() * (0.1 + random.nextDouble() * 0.3)));
				int direction = random.nextBoolean() ? 1 : -1;
				yield new CyclePricingStrategy(startPrice, amplitudeCents, duration, direction);
			}
		};
	}

	private static Money randomEndPrice(Money startPrice, Random random) {
		double factor = 0.5 + random.nextDouble();
		int endCents = Math.max(1, (int) (startPrice.cents() * factor));
		return new Money(endCents);
	}
}
