package net.peterv.bazillionaire.game.domain.ticker.regime;

import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

class LinearRegimeStrategyTest {

  private void check(Money initialPrice, int stepCents, int duration, int direction) {
    var regime = new LinearRegimeStrategy(initialPrice, stepCents, duration, direction);
    var prices = regime.prices();
    assertEquals(duration, prices.size(), "Regime should expose one price per tick");
    assertEquals(
        initialPrice.cents(), prices.get(0).cents(), "First price should equal initial price");

    int expectedRate = direction * stepCents;
    for (int t = 1; t < duration; t++) {
      assertEquals(
          expectedRate,
          prices.get(t).cents() - prices.get(t - 1).cents(),
          "Price change at tick " + t + " should be " + expectedRate);
    }
  }

  @Test
  void pricesIncreaseAtFixedRate() {
    check(new Money(100_00), 50, 100, 1);
  }

  @Test
  void pricesDecreaseAtFixedRate() {
    check(new Money(100_00), 50, 100, -1);
  }
}
