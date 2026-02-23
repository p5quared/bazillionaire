package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TickerTest {

	private static final Money INITIAL_PRICE = new Money(100_00);
	private static final int TOTAL_DURATION = 200;
	private static final int STRATEGY_DURATION = 50;
	private static final long SEED = 42L;

	private Ticker createTicker() {
		return new Ticker(INITIAL_PRICE, TOTAL_DURATION, STRATEGY_DURATION, new Random(SEED));
	}

	@Test
	void peekReturnsSamePricesAsSubsequentTicks() {
		var ticker = createTicker();

		int peekSize = 10;
		List<Money> peeked = ticker.peek(peekSize);
		assertEquals(peekSize, peeked.size());

		for (int i = 0; i < peekSize; i++) {
			ticker.tick();
			assertEquals(peeked.get(i), ticker.currentPrice(),
					"Peek price at offset %d does not match tick price".formatted(i));
		}
	}

	@Test
	void peekDoesNotMutateState() {
		var ticker = createTicker();
		ticker.tick();
		Money priceBefore = ticker.currentPrice();

		ticker.peek(20);
		ticker.peek(5);

		assertEquals(priceBefore, ticker.currentPrice(), "peek() should not change currentPrice");
	}

	@Test
	void strategyLinkageNoPriceDiscontinuities() {
		var ticker = createTicker();
		ticker.tick();
		Money prev = ticker.currentPrice();

		for (int i = 1; i < TOTAL_DURATION; i++) {
			ticker.tick();
			Money current = ticker.currentPrice();
			int jump = Math.abs(current.cents() - prev.cents());
			assertTrue(jump < prev.cents(),
					"Discontinuity at tick %d: %d -> %d (jump=%d)".formatted(i, prev.cents(), current.cents(), jump));
			prev = current;
		}
	}

	@Test
	void currentPriceBeforeFirstTickReturnsInitialPrice() {
		var ticker = createTicker();
		assertEquals(INITIAL_PRICE, ticker.currentPrice());
	}

	@Test
	void allPricesArePositive() {
		var ticker = createTicker();
		for (int i = 0; i < TOTAL_DURATION; i++) {
			ticker.tick();
			assertTrue(ticker.currentPrice().cents() > 0,
					"Price at tick %d should be positive".formatted(i));
		}
	}

	@Test
	void canFillBuyOrderAtOrBelowCurrentPrice() {
		var ticker = createTicker();
		var symbol = new Symbol("TEST");

		var exactBuy = new Order.Buy(symbol, INITIAL_PRICE);
		assertTrue(ticker.canFill(exactBuy), "Buy at current price should fill");

		var highBuy = new Order.Buy(symbol, new Money(INITIAL_PRICE.cents() + 100));
		assertTrue(ticker.canFill(highBuy), "Buy above current price should fill");

		var lowBuy = new Order.Buy(symbol, new Money(INITIAL_PRICE.cents() - 100));
		assertFalse(ticker.canFill(lowBuy), "Buy below current price should not fill");
	}

	@Test
	void canFillSellOrderAtOrAboveCurrentPrice() {
		var ticker = createTicker();
		var symbol = new Symbol("TEST");

		var exactSell = new Order.Sell(symbol, INITIAL_PRICE);
		assertTrue(ticker.canFill(exactSell), "Sell at current price should fill");

		var lowSell = new Order.Sell(symbol, new Money(INITIAL_PRICE.cents() - 100));
		assertTrue(ticker.canFill(lowSell), "Sell below current price should fill");

		var highSell = new Order.Sell(symbol, new Money(INITIAL_PRICE.cents() + 100));
		assertFalse(ticker.canFill(highSell), "Sell above current price should not fill");
	}
}
