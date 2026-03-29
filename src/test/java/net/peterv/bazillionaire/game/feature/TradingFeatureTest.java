package net.peterv.bazillionaire.game.feature;

import static net.peterv.bazillionaire.game.feature.GameAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

class TradingFeatureTest {

  @Test
  void buyAndSellCycle() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);
    assertFilled(h.buy("player1", symbol));
    assertFilled(h.sell("player1", symbol));
  }

  @Test
  void cannotSellWithoutHoldings() {
    var h = GameScenarios.singlePlayerStarted();
    assertRejected(h.sell("player1", h.symbols().get(0)));
  }

  @Test
  void unknownPlayerCannotTrade() {
    var h = GameScenarios.singlePlayerStarted();
    assertInvalidOrder(h.buy("stranger", h.symbols().get(0)));
  }

  @Test
  void invalidSymbolIsRejected() {
    var h = GameScenarios.singlePlayerStarted();
    assertInvalidOrder(h.buy("player1", "FAKE"));
  }

  @Test
  void buyingEventuallyExhaustsFunds() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);
    OrderResult rejection = h.buyUntilRejected("player1", symbol);
    assertInstanceOf(OrderResult.class, rejection);
    assertFalse(rejection instanceof OrderResult.Filled);
  }

  @Test
  void filledBuyEmitsOrderFilledAndOrderActivity() {
    var h = GameScenarios.singlePlayerStarted();
    int checkpoint = h.messageCheckpoint();
    h.buy("player1", h.symbols().get(0));
    assertHasEventSince(h, GameEvent.OrderFilled.class, checkpoint);
    assertHasEventSince(h, GameEvent.OrderActivity.class, checkpoint);
  }

  @Test
  void orderFilledIsPrivateAndOrderActivityIsBroadcast() {
    var h = GameScenarios.singlePlayerStarted();
    int checkpoint = h.messageCheckpoint();
    h.buy("player1", h.symbols().get(0));

    var filledMessages =
        h.messagesSince(checkpoint).stream()
            .filter(m -> m.event() instanceof GameEvent.OrderFilled)
            .toList();
    assertFalse(filledMessages.isEmpty());
    filledMessages.forEach(m -> assertIsPrivateTo(m, "player1"));

    var activityMessages =
        h.messagesSince(checkpoint).stream()
            .filter(m -> m.event() instanceof GameEvent.OrderActivity)
            .toList();
    assertFalse(activityMessages.isEmpty());
    activityMessages.forEach(GameAssertions::assertIsBroadcast);
  }

  @Test
  void firstBuyCostBasisEqualsFillPrice() {
    var h = GameScenarios.singlePlayerStarted();
    var fill = assertFilled(h.buy("player1", h.symbols().get(0)));
    assertEquals(fill.fillPrice(), fill.costBasis());
  }

  @Test
  void costBasisAveragesAcrossMultipleBuys() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    var fill1 = assertFilled(h.buy("player1", symbol));
    assertEquals(fill1.fillPrice(), fill1.costBasis());

    h.tick();
    var fill2 = assertFilled(h.buy("player1", symbol));
    Money expectedAvg = new Money((fill1.fillPrice().cents() + fill2.fillPrice().cents()) / 2);
    assertEquals(expectedAvg, fill2.costBasis());
  }

  @Test
  void costBasisPreservedOnPartialSell() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    assertFilled(h.buy("player1", symbol));
    var fill2 = assertFilled(h.buy("player1", symbol));
    Money costBasisBeforeSell = fill2.costBasis();

    h.tick();
    var sellFill = assertFilled(h.sell("player1", symbol));
    assertEquals(costBasisBeforeSell, sellFill.costBasis());
  }

  @Test
  void costBasisResetsWhenPositionFullyClosed() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    assertFilled(h.buy("player1", symbol));
    var sellFill = assertFilled(h.sell("player1", symbol));
    assertEquals(new Money(0), sellFill.costBasis());
  }

  @Test
  void costBasisResetsAfterSellingAllThenBuyingAgain() {
    var h = GameScenarios.singlePlayerStarted();
    String symbol = h.symbols().get(0);

    assertFilled(h.buy("player1", symbol));
    assertFilled(h.sell("player1", symbol));

    h.tick();
    var rebuy = assertFilled(h.buy("player1", symbol));
    assertEquals(rebuy.fillPrice(), rebuy.costBasis());
  }
}
