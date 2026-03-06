package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import static org.junit.jupiter.api.Assertions.*;

class RegimeStrategyTestHelper {

	static void assertBoundedConvergence(
			RegimeStrategy regime, Money startPrice, Money endPrice, int duration) {
		int minCents = Math.min(startPrice.cents(), endPrice.cents());
		int maxCents = Math.max(startPrice.cents(), endPrice.cents());
		for (int t = 0; t < duration - 1; t++) {
			int price = regime.priceAt(t).cents();
			assertTrue(price >= minCents && price <= maxCents,
					"Price %d at tick %d out of range [%d, %d]".formatted(price, t, minCents, maxCents));
		}
		assertEquals(endPrice.cents(), regime.priceAt(duration - 1).cents(),
				"Last tick should return endPrice");
	}

	static int[] drainPrices(RegimeStrategy regime, int ticks) {
		int[] prices = new int[ticks];
		for (int i = 0; i < ticks; i++) {
			prices[i] = regime.priceAt(i).cents();
		}
		return prices;
	}
}
