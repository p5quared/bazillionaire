package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.ticker.MarketForce;
import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CyclePricingStrategyTest {

	@Test
	void pricesStayWithinBoundedRange() {
		var initialPrice = new Money(100_00);
		int stepCents = 1_00;
		int cycleDuration = 100;
		var strategy = new CyclePricingStrategy(initialPrice, stepCents, cycleDuration, 1);

		int minExpected = initialPrice.cents();
		int maxExpected = initialPrice.cents() + (stepCents * cycleDuration);

		boolean allWithinRange = IntStream.range(0, cycleDuration)
				.mapToObj(i -> strategy.nextPrice(MarketForce.neutral()))
				.map(Money::cents)
				.allMatch(p -> p <= maxExpected && p >= minExpected);

		assertEquals(StrategyKind.CYCLE, strategy.kind());
		assertTrue(strategy.isExhausted(), "Strategy is exhausted");
		assertTrue(allWithinRange, "Prices escape range");
	}

	@Test
	void pricesStayWithinBoundedRangeNegative() {
		var initialPrice = new Money(100_00);
		int stepCents = 1_00;
		int cycleDuration = 100;
		var strategy = new CyclePricingStrategy(initialPrice, stepCents, cycleDuration, -1);

		int maxExpected = initialPrice.cents();
		int minExpected = initialPrice.cents() - (stepCents * cycleDuration);

		boolean allWithinRange = IntStream.range(0, cycleDuration)
				.mapToObj(i -> strategy.nextPrice(MarketForce.neutral()))
				.map(Money::cents)
				.allMatch(p -> p <= maxExpected && p >= minExpected);

		assertEquals(StrategyKind.CYCLE, strategy.kind());
		assertTrue(strategy.isExhausted(), "Strategy is exhausted");
		assertTrue(allWithinRange, "Prices escape range");
	}
}
