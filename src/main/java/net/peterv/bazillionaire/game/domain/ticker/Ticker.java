package net.peterv.bazillionaire.game.domain.ticker;

import java.util.ArrayList;
import java.util.List;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeStrategy;
import net.peterv.bazillionaire.game.domain.types.Money;

public class Ticker {
  private final RegimeFactory regimeFactory;
  private List<RegimeStrategy> regimes = new ArrayList<>();
  private int cursor = 0;

  public Ticker(RegimeFactory regimeFactory, Money initialPrice) {
    this.regimeFactory = regimeFactory;
    this.regimes.add(regimeFactory.nextRegime(initialPrice));
  }

  public Money currentPrice() {
    return regimes.getLast().prices().get(cursor);
  }

  public void tick() {
    cursor++;
    if (cursor >= regimes.getLast().prices().size()) {
      Money lastPrice = regimes.getLast().prices().getLast();
      this.regimes.add(this.regimeFactory.nextRegime(lastPrice));
      cursor = 0;
    }
  }
}
