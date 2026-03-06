package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import static org.junit.jupiter.api.Assertions.*;

class RegimeStrategyTestHelper {

	static void assertBoundedConvergence(
			RegimeStrategy regime, Money startPrice, Money endPrice, int duration) {
		var prices = regime.prices();
		assertEquals(duration, prices.size(), "Regime should expose one price per tick");

		int minCents = Math.min(startPrice.cents(), endPrice.cents());
		int maxCents = Math.max(startPrice.cents(), endPrice.cents());
		for (int t = 0; t < duration - 1; t++) {
			int price = prices.get(t).cents();
			assertTrue(price >= minCents && price <= maxCents,
					"Price %d at tick %d out of range [%d, %d]".formatted(price, t, minCents, maxCents));
		}
		assertEquals(endPrice.cents(), prices.get(duration - 1).cents(),
				"Last tick should return endPrice");
	}

	static int[] drainPrices(RegimeStrategy regime, int ticks) {
		var regimePrices = regime.prices();
		int[] prices = new int[ticks];
		for (int i = 0; i < ticks; i++) {
			prices[i] = regimePrices.get(i).cents();
		}
		return prices;
	}
}
