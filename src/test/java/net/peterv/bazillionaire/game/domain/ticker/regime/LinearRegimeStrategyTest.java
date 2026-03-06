package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinearRegimeStrategyTest {

	private void check(
			Money initialPrice,
			int stepCents,
			int duration,
			int direction) {
		var regime = new LinearRegimeStrategy(initialPrice, stepCents, duration, direction);
		var prices = regime.prices();
		assertEquals(duration, prices.size(), "Regime should expose one price per tick");
		int expectedRate = direction * stepCents;

		int previousCents = initialPrice.cents();
		for (int t = 0; t < duration - 1; t++) {
			int price = prices.get(t).cents();
			assertEquals(expectedRate, price - previousCents,
					"Price change at tick " + t + " should be " + expectedRate);
			previousCents = price;
		}

		// Final tick holds price
		int lastPrice = prices.get(duration - 1).cents();
		assertEquals(previousCents, lastPrice, "Last tick should hold price");
	}

	@Test
	void pricesIncreaseAtFixedRate() {
		check(new Money(100_00), 50, 100, 1);
	}

	@Test
	void pricesDecreaseAtFixedRate() {
		check(new Money(100_00), 50, 100, -1);
	}
}
