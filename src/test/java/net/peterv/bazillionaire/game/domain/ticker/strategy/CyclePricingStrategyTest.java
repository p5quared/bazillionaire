package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CyclePricingStrategyTest {

	private void check(
			Money initialPrice,
			int amplitudeCents,
			int cycleDuration,
			int direction,
			int minExpected,
			int maxExpected) {
		var strategy = new CyclePricingStrategy(initialPrice, amplitudeCents, cycleDuration, direction);

		boolean allWithinRange = IntStream.range(0, cycleDuration)
				.mapToObj(t -> strategy.priceAt(t))
				.map(Money::cents)
				.allMatch(p -> p <= maxExpected && p >= minExpected);

		assertTrue(allWithinRange, "Prices escape range");
	}

	@Test
	void pricesStayWithinBoundedRange() {
		var initialPrice = new Money(100_00);
		int amplitudeCents = 50_00;
		int cycleDuration = 100;
		check(initialPrice, amplitudeCents, cycleDuration, 1,
				initialPrice.cents() - amplitudeCents,
				initialPrice.cents() + amplitudeCents);
	}

	@Test
	void pricesStayWithinBoundedRangeNegative() {
		var initialPrice = new Money(100_00);
		int amplitudeCents = 50_00;
		int cycleDuration = 100;
		check(initialPrice, amplitudeCents, cycleDuration, -1,
				initialPrice.cents() - amplitudeCents,
				initialPrice.cents() + amplitudeCents);
	}
}
