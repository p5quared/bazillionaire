package net.peterv.bazillionaire.game.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class Portfolio {
  private Money cashBalance;
  private final Map<Symbol, Integer> holdings;
  private final Map<Symbol, Money> totalCost;

  public Portfolio(Money initialBalance) {
    this.cashBalance = initialBalance;
    this.holdings = new HashMap<>();
    this.totalCost = new HashMap<>();
  }

  public OrderResult fill(Order order, Money fillPrice) {
    return switch (order) {
      case Order.Buy buy -> tryBuy(buy, fillPrice);
      case Order.Sell sell -> trySell(sell, fillPrice);
    };
  }

  private OrderResult tryBuy(Order.Buy buy, Money fillPrice) {
    if (!cashBalance.isGreaterThanOrEqualTo(fillPrice)) {
      return new OrderResult.Rejected("Insufficient funds");
    }
    holdings.merge(buy.symbol(), 1, Integer::sum);
    totalCost.merge(buy.symbol(), fillPrice, Money::add);
    cashBalance = cashBalance.subtract(fillPrice);
    return new OrderResult.Filled(fillPrice, costBasisOf(buy.symbol()));
  }

  private OrderResult trySell(Order.Sell sell, Money fillPrice) {
    if (holdingsOf(sell.symbol()) < 1) {
      return new OrderResult.Rejected("No shares of %s to sell".formatted(sell.symbol().value()));
    }
    Money currentCostBasis = costBasisOf(sell.symbol());
    holdings.merge(sell.symbol(), -1, Integer::sum);
    int remaining = holdingsOf(sell.symbol());
    if (remaining == 0) {
      totalCost.remove(sell.symbol());
    } else {
      totalCost.put(sell.symbol(), new Money(currentCostBasis.cents() * remaining));
    }
    cashBalance = cashBalance.add(fillPrice);
    return new OrderResult.Filled(fillPrice, remaining == 0 ? new Money(0) : currentCostBasis);
  }

  public Money costBasisOf(Symbol symbol) {
    int qty = holdingsOf(symbol);
    if (qty == 0) {
      return new Money(0);
    }
    return new Money(totalCost.getOrDefault(symbol, new Money(0)).cents() / qty);
  }

  public int holdingsOf(Symbol symbol) {
    return holdings.getOrDefault(symbol, 0);
  }

  public void addCash(Money amount) {
    this.cashBalance = this.cashBalance.add(amount);
  }

  public Money cashBalance() {
    return cashBalance;
  }

  public Map<Symbol, Integer> holdings() {
    return Collections.unmodifiableMap(holdings);
  }
}
