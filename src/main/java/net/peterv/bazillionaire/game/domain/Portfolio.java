package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.domain.order.OrderResult;

import java.util.HashMap;
import java.util.Map;

public class Portfolio {
	private Money cashBalance;
	private final Map<Symbol, Integer> holdings;

	public Portfolio(Money initialBalance) {
		this.cashBalance = initialBalance;
		this.holdings = new HashMap<>();
	}

	public OrderResult fill(Order order) {
		return switch (order) {
			case Order.Buy buy -> tryBuy(buy);
			case Order.Sell sell -> trySell(sell);
		};
	}

	private OrderResult tryBuy(Order.Buy buy) {
		if (!cashBalance.isGreaterThanOrEqualTo(buy.price())) {
			return new OrderResult.Rejected("Insufficient funds");
		}
		holdings.merge(buy.symbol(), 1, Integer::sum);
		cashBalance = cashBalance.subtract(buy.price());
		return new OrderResult.Filled();
	}

	private OrderResult trySell(Order.Sell sell) {
		if (holdingsOf(sell.symbol()) < 1) {
			return new OrderResult.Rejected("No shares of %s to sell".formatted(sell.symbol().value()));
		}
		holdings.merge(sell.symbol(), -1, Integer::sum);
		cashBalance = cashBalance.add(sell.price());
		return new OrderResult.Filled();
	}

	public int holdingsOf(Symbol symbol) {
		return holdings.getOrDefault(symbol, 0);
	}
}
