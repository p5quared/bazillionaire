package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class GameFillRateLimitTest {

  /** High balance so rate-limit tests aren't constrained by funds. */
  private static final Money RATE_LIMIT_BALANCE = new Money(100_000_00);

  private Game startedGameWithHighBalance(PlayerId... players) {
    Game game =
        GameFactory.create(
            List.of(players), TICKER_COUNT, RATE_LIMIT_BALANCE, TOTAL_DURATION, new Random(SEED));
    game.drainMessages();
    for (PlayerId player : players) {
      game.join(player);
    }
    game.start();
    game.drainMessages();
    return game;
  }

  private Symbol anySymbol(Game game) {
    return game.currentPrices().keySet().iterator().next();
  }

  @Test
  void fillsWithinCapAreFilled() {
    var game = startedGameWithHighBalance(PLAYER_1);
    var symbol = anySymbol(game);
    for (int i = 0; i < 25; i++) {
      var result = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      assertInstanceOf(OrderResult.Filled.class, result, "Fill #" + i + " should succeed");
      game.drainMessages();
    }
  }

  @Test
  void exceedingCapOnOneTickerRejectsButOtherTickersStillFillable() {
    var game = startedGameWithHighBalance(PLAYER_1);
    var symbols = game.currentPrices().keySet().stream().toList();
    var symbol1 = symbols.get(0);
    var symbol2 = symbols.get(1);

    for (int i = 0; i < 25; i++) {
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
    var game = startedGameWithHighBalance(PLAYER_1);
    var symbol = anySymbol(game);

    for (int i = 0; i < 13; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }
    for (int i = 0; i < 12; i++) {
      game.placeOrder(new Order.Sell(symbol), PLAYER_1);
      game.drainMessages();
    }

    var rejected = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, rejected);
    assertEquals("Volume limit exceeded", ((OrderResult.Rejected) rejected).reason());
  }

  @Test
  void fillsAllowedAgainAfterTokensRefill() {
    var game = startedGameWithHighBalance(PLAYER_1);
    var symbol = anySymbol(game);

    for (int i = 0; i < 25; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }

    var rejected = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, rejected);

    // 6 ticks needed: first onTick(0) has elapsed=0 (no refill),
    // then 5 ticks of elapsed=1 each → 5 * (1/5) = 1 token
    for (int i = 0; i < 6; i++) {
      game.tick();
      game.drainMessages();
    }

    var filled = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    assertInstanceOf(OrderResult.Filled.class, filled);
  }
}
