package net.peterv.bazillionaire.game.domain.ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeStrategy;
import net.peterv.bazillionaire.game.domain.types.Money;

public class Ticker {
  private final RegimeFactory regimeFactory;
  private List<RegimeStrategy> regimes = new ArrayList<>();
  private int cursor = 0;

  public Ticker(Money initialPrice, Random random) {
    this.regimeFactory = new RegimeFactory(initialPrice, random);
    this.regimes.add(this.regimeFactory.nextRegime());
  }

  public boolean canFill(Order order) {
    return switch (order) {
      case Order.Buy o -> o.price().isGreaterThanOrEqualTo(this.currentPrice());
      case Order.Sell o -> this.currentPrice().isGreaterThanOrEqualTo(o.price());
    };
  }

  public Money currentPrice() {
    return regimes.getLast().prices().get(cursor);
  }

  public void tick() {
    cursor++;
    if (cursor >= regimes.getLast().prices().size()) {
      this.regimes.add(this.regimeFactory.nextRegime());
      cursor = 0;
    }
  }
}
