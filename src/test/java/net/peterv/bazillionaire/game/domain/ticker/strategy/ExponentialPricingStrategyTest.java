package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExponentialPricingStrategyTest {

	private void check(Money startPrice, Money endPrice, int duration, double curvature) {
		var strategy = new ExponentialPricingStrategy(startPrice, endPrice, duration, curvature);
		int minCents = Math.min(startPrice.cents(), endPrice.cents());
		int maxCents = Math.max(startPrice.cents(), endPrice.cents());

		for (int i = 0; i < duration - 1; i++) {
			int price = strategy.nextPrice().cents();
			assertTrue(price >= minCents && price <= maxCents,
					"Price %d at tick %d out of range [%d, %d]".formatted(price, i + 1, minCents, maxCents));
		}

		int lastPrice = strategy.nextPrice().cents();
		assertEquals(endPrice.cents(), lastPrice, "Exhausted tick should return endPrice");
		assertEquals(StrategyKind.EXPONENTIAL, strategy.kind());
		assertTrue(strategy.isExhausted(), "Strategy should be exhausted");
	}

	@Test
	void ascendingPriceStaysInRange() {
		check(new Money(100_00), new Money(200_00), 100, 4.0);
	}

	@Test
	void descendingPriceStaysInRange() {
		check(new Money(200_00), new Money(100_00), 100, 4.0);
	}

	@Test
	void laterTicksChangeMoreThanEarlyTicks() {
		var strategy = new ExponentialPricingStrategy(new Money(0), new Money(100_00), 100, 5.0);

		int[] prices = new int[100];
		for (int i = 0; i < 100; i++) {
			prices[i] = strategy.nextPrice().cents();
		}

		int earlyChange = prices[19] - 0;
		int lateChange = prices[99] - prices[79];

		assertTrue(lateChange > earlyChange,
				"Late change (%d) should exceed early change (%d)".formatted(lateChange, earlyChange));
	}
}
