package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.order.OrderResult;
import org.junit.jupiter.api.Test;

class VolumeLimitFeatureTest {

  @Test
  void rapidTradingEventuallyHitsLimit() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);
    OrderResult result = h.buyUntilRejected("player1", symbol);
    assertInstanceOf(OrderResult.Rejected.class, result);
  }

  @Test
  void limitIsPerTicker() {
    var h = GameScenarios.singlePlayerStarted();
    assertTrue(h.symbols().size() >= 2, "Need at least 2 symbols for this test");

    String sym1 = h.symbols().get(0);
    String sym2 = h.symbols().get(1);

    h.buyUntilRejected("player1", sym1);
    assertFilled(h.buy("player1", sym2));
  }

  @Test
  void limitRefillsOverTime() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    h.buyUntilRejected("player1", symbol);
    assertRejected(h.buy("player1", symbol));

    // Tick until we can trade again (generous upper bound)
    for (int i = 0; i < 200; i++) {
      h.tick();
      if (h.buy("player1", symbol).result() instanceof OrderResult.Filled) {
        return; // success — limit refilled
      }
    }
    fail("Volume limit did not refill after 200 ticks");
  }
}
