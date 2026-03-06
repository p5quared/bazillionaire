package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegimeFactoryTest {

	private static final Money INITIAL_PRICE = new Money(100_00);
	private static final int REGIME_DURATION = 50;
	private static final long SEED = 42L;

	@Test
	void eachRegimeUsesConfiguredDuration() {
		var factory = new RegimeFactory(INITIAL_PRICE, REGIME_DURATION, new Random(SEED));

		assertEquals(REGIME_DURATION, factory.nextRegime().prices().size());
		assertEquals(REGIME_DURATION, factory.nextRegime().prices().size());
	}

	@Test
	void successiveRegimesRemainPositive() {
		var factory = new RegimeFactory(INITIAL_PRICE, REGIME_DURATION, new Random(SEED));

		List<Money> first = factory.nextRegime().prices();
		List<Money> second = factory.nextRegime().prices();

		assertTrue(first.get(first.size() - 1).cents() > 0);
		assertTrue(second.get(second.size() - 1).cents() > 0);
	}

	@Test
	void seededFactoryProducesDeterministicRegimes() {
		var firstFactory = new RegimeFactory(INITIAL_PRICE, REGIME_DURATION, new Random(SEED));
		var secondFactory = new RegimeFactory(INITIAL_PRICE, REGIME_DURATION, new Random(SEED));

		List<Money> firstPrices = firstFactory.nextRegime().prices();
		List<Money> secondPrices = secondFactory.nextRegime().prices();

		assertIterableEquals(firstPrices, secondPrices);
	}
}
