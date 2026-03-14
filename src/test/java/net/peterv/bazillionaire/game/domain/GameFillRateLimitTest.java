package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import org.junit.jupiter.api.Test;

class GameFillRateLimitTest {

  @Test
  void fillsWithinCapAreFilled() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);
    for (int i = 0; i < 10; i++) {
      var result = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      assertInstanceOf(OrderResult.Filled.class, result, "Fill #" + i + " should succeed");
      game.drainMessages();
    }
  }

  @Test
  void exceedingCapOnOneTickerRejectsButOtherTickersStillFillable() {
    var game = startedGame(PLAYER_1);
    var symbols = game.currentPrices().keySet().stream().toList();
    var symbol1 = symbols.get(0);
    var symbol2 = symbols.get(1);

    for (int i = 0; i < 10; i++) {
      game.placeOrder(new Order.Buy(symbol1), PLAYER_1);
      game.drainMessages();
    }

    var rejected = game.placeOrder(new Order.Buy(symbol1), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, rejected);
    assertEquals("Volume limit exceeded", ((OrderResult.Rejected) rejected).reason());

    var filled = game.placeOrder(new Order.Buy(symbol2), PLAYER_1);
    assertInstanceOf(OrderResult.Filled.class, filled);
    game.drainMessages();
  }

  @Test
  void buysAndSellsBothCountTowardCap() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);

    for (int i = 0; i < 5; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }
    for (int i = 0; i < 5; i++) {
      game.placeOrder(new Order.Sell(symbol), PLAYER_1);
      game.drainMessages();
    }

    var rejected = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, rejected);
    assertEquals("Volume limit exceeded", ((OrderResult.Rejected) rejected).reason());
  }

  @Test
  void fillsAllowedAgainAfterWindowExpires() {
    var game = startedGame(PLAYER_1);
    var symbol = anySymbol(game);

    for (int i = 0; i < 10; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }

    var rejected = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, rejected);

    for (int i = 0; i < 100; i++) {
      game.tick();
      game.drainMessages();
    }

    var filled = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Filled.class, filled);
  }
}
