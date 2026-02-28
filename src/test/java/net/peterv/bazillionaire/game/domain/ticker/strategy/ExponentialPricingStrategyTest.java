package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExponentialPricingStrategyTest {

	private void check(Money startPrice, Money endPrice, int duration, double curvature) {
		var strategy = new ExponentialPricingStrategy(startPrice, endPrice, duration, curvature);
		PricingStrategyTestHelper.assertBoundedConvergence(strategy, startPrice, endPrice, duration, StrategyKind.EXPONENTIAL);
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

		int[] prices = PricingStrategyTestHelper.drainPrices(strategy, 100);

		int earlyChange = prices[19] - 0;
		int lateChange = prices[99] - prices[79];

		assertTrue(lateChange > earlyChange,
				"Late change (%d) should exceed early change (%d)".formatted(lateChange, earlyChange));
	}
}
