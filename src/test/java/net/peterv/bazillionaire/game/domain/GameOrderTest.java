package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class GameOrderTest {

  private static final PlayerId UNKNOWN = new PlayerId("unknown");

  @SafeVarargs
  private void assertOrder(
      Game game,
      Order order,
      PlayerId playerId,
      Class<? extends OrderResult> expectedResult,
      Class<? extends GameEvent>... expectedEvents) {
    var result = game.placeOrder(order, playerId);
    assertInstanceOf(expectedResult, result);
    var messages = game.drainMessages();
    assertEquals(expectedEvents.length, messages.size());
    for (int i = 0; i < expectedEvents.length; i++) {
      assertInstanceOf(expectedEvents[i], messages.get(i).event());
    }
  }

  @Test
  void invalidOrders() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);
    assertOrder(game, new Order.Buy(symbol), UNKNOWN, OrderResult.InvalidOrder.class);
    assertOrder(game, new Order.Buy(new Symbol("FAKE")), PLAYER_1, OrderResult.InvalidOrder.class);
  }

  @Test
  void buyOrders() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);
    assertOrder(
        game,
        new Order.Buy(symbol),
        PLAYER_1,
        OrderResult.Filled.class,
        GameEvent.OrderFilled.class,
        GameEvent.PlayersState.class);

    var result = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertEquals(INITIAL_PRICE, result.fillPrice());
    assertEquals(INITIAL_PRICE, result.costBasis());
    game.drainMessages();
  }

  @Test
  void buyOrderRejectsWhenInsufficientFunds() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);
    // Buy until we run out of money
    int maxBuys = INITIAL_BALANCE.cents() / INITIAL_PRICE.cents();
    for (int i = 0; i < maxBuys; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }
    assertOrder(game, new Order.Buy(symbol), PLAYER_1, OrderResult.Rejected.class);
  }

  @Test
  void sellOrders() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);
    assertOrder(game, new Order.Sell(symbol), PLAYER_1, OrderResult.Rejected.class);

    game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();

    assertOrder(
        game,
        new Order.Sell(symbol),
        PLAYER_1,
        OrderResult.Filled.class,
        GameEvent.OrderFilled.class,
        GameEvent.PlayersState.class);
  }

  @Test
  void costBasisAveragesAcrossMultipleBuys() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);

    var fill1 = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    Money price1 = fill1.fillPrice();
    assertEquals(price1, fill1.costBasis(), "Cost basis after 1 buy should equal fill price");

    game.tick();
    game.drainMessages();
    var fill2 = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    Money price2 = fill2.fillPrice();
    Money expectedBasis2 = new Money((price1.cents() + price2.cents()) / 2);
    assertEquals(expectedBasis2, fill2.costBasis(), "Cost basis should average 2 fill prices");

    game.tick();
    game.drainMessages();
    var fill3 = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    Money price3 = fill3.fillPrice();
    Money expectedBasis3 = new Money((price1.cents() + price2.cents() + price3.cents()) / 3);
    assertEquals(expectedBasis3, fill3.costBasis(), "Cost basis should average 3 fill prices");
  }

  @Test
  void costBasisPreservedOnSellAndResetsWhenFullySold() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);

    var buy1 = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    Money costBasis = buy1.costBasis();

    game.tick();
    game.drainMessages();
    var sell1 = (OrderResult.Filled) game.placeOrder(new Order.Sell(symbol), PLAYER_1);
    game.drainMessages();
    assertEquals(costBasis, sell1.costBasis(), "Cost basis per share should not change on sell");

    var sell2 = (OrderResult.Filled) game.placeOrder(new Order.Sell(symbol), PLAYER_1);
    game.drainMessages();
    assertEquals(new Money(0), sell2.costBasis(), "Cost basis should reset to 0 when fully sold");
  }

  @Test
  void costBasisResetsAfterSellingAllThenBuyingAgain() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);

    game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    game.placeOrder(new Order.Sell(symbol), PLAYER_1);
    game.drainMessages();

    game.tick();
    game.drainMessages();
    var rebuy = (OrderResult.Filled) game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();
    assertEquals(
        rebuy.fillPrice(),
        rebuy.costBasis(),
        "Cost basis should be the new fill price, not influenced by prior position");
  }
}
