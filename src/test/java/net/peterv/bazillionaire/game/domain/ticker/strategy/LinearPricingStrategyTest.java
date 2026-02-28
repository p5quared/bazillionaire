package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinearPricingStrategyTest {

	private void check(
			Money initialPrice,
			int stepCents,
			int duration,
			int direction) {
		var strategy = new LinearPricingStrategy(initialPrice, stepCents, duration, direction);
		int expectedRate = direction * stepCents;

		int previousCents = initialPrice.cents();
		for (int t = 0; t < duration - 1; t++) {
			int price = strategy.priceAt(t).cents();
			assertEquals(expectedRate, price - previousCents,
					"Price change at tick " + t + " should be " + expectedRate);
			previousCents = price;
		}

		// Final tick holds price
		int lastPrice = strategy.priceAt(duration - 1).cents();
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
