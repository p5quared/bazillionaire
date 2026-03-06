package net.peterv.bazillionaire.game.domain.ticker;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.List;
import java.util.Random;

public class Ticker {
	private final Money initialPrice;
	private final RegimeFactory regimeFactory;
	private List<Money> currentRegimePrices = List.of();
	private int cursor = -1;

	public Ticker(Money initialPrice, int regimeDuration, Random random) {
		this.initialPrice = initialPrice;
		this.regimeFactory = new RegimeFactory(initialPrice, regimeDuration, random);
	}

	public boolean canFill(Order order) {
		return switch (order) {
			case Order.Buy o -> o.price().isGreaterThanOrEqualTo(this.currentPrice());
			case Order.Sell o -> this.currentPrice().isGreaterThanOrEqualTo(o.price());
		};
	}

	public Money currentPrice() {
		if (cursor < 0) {
			return initialPrice;
		}
		return currentRegimePrices.get(cursor);
	}

	public void tick() {
		if (currentRegimePrices.isEmpty() || cursor >= currentRegimePrices.size() - 1) {
			currentRegimePrices = regimeFactory.nextRegime().prices();
			cursor = 0;
			return;
		}
		cursor++;
	}
}
