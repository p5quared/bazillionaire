package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogisticPricingStrategyTest {

	private void check(Money startPrice, Money endPrice, int duration, double steepness) {
		var strategy = new LogisticPricingStrategy(startPrice, endPrice, duration, steepness);
		PricingStrategyTestHelper.assertBoundedConvergence(strategy, startPrice, endPrice, duration, StrategyKind.LOGISTIC);
	}

	@Test
	void ascendingPriceStaysInRange() {
		check(new Money(100_00), new Money(200_00), 100, 8.0);
	}

	@Test
	void descendingPriceStaysInRange() {
		check(new Money(200_00), new Money(100_00), 100, 8.0);
	}

	@Test
	void middleTicksChangeMoreThanEdgeTicks() {
		var strategy = new LogisticPricingStrategy(new Money(0), new Money(100_00), 100, 10.0);

		int[] prices = PricingStrategyTestHelper.drainPrices(strategy, 100);

		int earlyChange = prices[19] - 0;
		int middleChange = prices[59] - prices[39];
		int lateChange = prices[99] - prices[79];

		assertTrue(middleChange > earlyChange,
				"Middle change (%d) should exceed early change (%d)".formatted(middleChange, earlyChange));
		assertTrue(middleChange > lateChange,
				"Middle change (%d) should exceed late change (%d)".formatted(middleChange, lateChange));
	}
}
