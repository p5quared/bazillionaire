package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTestHelper {

	static void assertBoundedConvergence(
			PricingStrategy strategy, Money startPrice, Money endPrice, int duration) {
		int minCents = Math.min(startPrice.cents(), endPrice.cents());
		int maxCents = Math.max(startPrice.cents(), endPrice.cents());
		for (int t = 0; t < duration - 1; t++) {
			int price = strategy.priceAt(t).cents();
			assertTrue(price >= minCents && price <= maxCents,
					"Price %d at tick %d out of range [%d, %d]".formatted(price, t, minCents, maxCents));
		}
		assertEquals(endPrice.cents(), strategy.priceAt(duration - 1).cents(),
				"Last tick should return endPrice");
	}

	static int[] drainPrices(PricingStrategy strategy, int ticks) {
		int[] prices = new int[ticks];
		for (int i = 0; i < ticks; i++) {
			prices[i] = strategy.priceAt(i).cents();
		}
		return prices;
	}
}
