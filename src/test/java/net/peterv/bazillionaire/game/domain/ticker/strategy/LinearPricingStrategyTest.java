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
		for (int i = 0; i < duration - 1; i++) {
			int price = strategy.nextPrice().cents();
			assertEquals(expectedRate, price - previousCents,
					"Price change at tick " + (i + 1) + " should be " + expectedRate);
			previousCents = price;
		}

		// Final tick triggers exhaustion — price holds
		int lastPrice = strategy.nextPrice().cents();
		assertEquals(previousCents, lastPrice, "Exhausted tick should hold price");
		assertEquals(StrategyKind.LINEAR, strategy.kind());
		assertTrue(strategy.isExhausted(), "Strategy should be exhausted");
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
