package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
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
    assertEquals(INITIAL_PRICE, result.price());
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
}
