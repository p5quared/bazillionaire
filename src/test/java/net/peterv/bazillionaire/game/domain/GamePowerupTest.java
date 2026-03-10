package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.ConsumptionMode;
import net.peterv.bazillionaire.game.domain.powerup.OrderFreezePowerup;
import net.peterv.bazillionaire.game.domain.powerup.OrderInterceptor;
import net.peterv.bazillionaire.game.domain.powerup.Powerup;
import net.peterv.bazillionaire.game.domain.powerup.PowerupEffect;
import net.peterv.bazillionaire.game.domain.powerup.PowerupUsageType;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class GamePowerupTest {

  static class BlockingInterceptor extends Powerup implements OrderInterceptor {
    BlockingInterceptor() {
      super(5);
    }

    @Override
    public String name() {
      return "blocking-interceptor";
    }

    @Override
    public String description() {
      return "test";
    }

    @Override
    public PowerupUsageType usageType() {
      return PowerupUsageType.INSTANT;
    }

    @Override
    public ConsumptionMode consumptionMode() {
      return ConsumptionMode.SINGLE;
    }

    @Override
    public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) {
      return new OrderResult.Rejected("blocked by powerup");
    }
  }

  static class TickCountingPowerup extends Powerup {
    int onTickCount = 0;

    TickCountingPowerup(int duration) {
      super(duration);
    }

    @Override
    public String name() {
      return "tick-counting";
    }

    @Override
    public String description() {
      return "test";
    }

    @Override
    public PowerupUsageType usageType() {
      return PowerupUsageType.INSTANT;
    }

    @Override
    public ConsumptionMode consumptionMode() {
      return ConsumptionMode.SINGLE;
    }

    @Override
    public List<PowerupEffect> onTick() {
      onTickCount++;
      return List.of();
    }
  }

  @Test
  void blockingInterceptorPreventsOrderFill() {
    Game game = startedGame(PLAYER_1);
    Symbol symbol = anySymbol(game);

    game.activatePowerup(new BlockingInterceptor());

    OrderResult result = game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
    assertInstanceOf(OrderResult.Rejected.class, result);
    assertTrue(game.drainMessages().isEmpty());
  }

  @Test
  void tickCountingPowerupReceivesOnTickPerGameTick() {
    Game game = startedGame(PLAYER_1);
    var powerup = new TickCountingPowerup(10);
    game.activatePowerup(powerup);

    game.tick();
    game.tick();
    game.tick();

    assertEquals(3, powerup.onTickCount);
  }

  @Test
  void noActivePowerupsDoesNotAffectNormalOrderFlow() {
    Game game = startedGame(PLAYER_1);
    Symbol symbol = anySymbol(game);

    OrderResult result = game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
    assertInstanceOf(OrderResult.Filled.class, result);
  }

  @Test
  void noActivePowerupsDoesNotAffectNormalTickFlow() {
    Game game = startedGame(PLAYER_1);

    game.tick();

    assertEquals(1, game.currentTick());
  }

  @Test
  void orderFreezeBlocksOrdersUntilExpiry() {
    PlayerId frozenPlayer = new PlayerId("player1");
    PlayerId otherPlayer = new PlayerId("player2");
    Symbol symbol = new Symbol("ABC");
    Game game =
        new Game(
            Map.of(
                frozenPlayer, new Portfolio(INITIAL_BALANCE),
                otherPlayer, new Portfolio(INITIAL_BALANCE)),
            Map.of(symbol, new Ticker(INITIAL_PRICE, new Random(SEED))),
            TOTAL_DURATION);
    game.start();
    game.drainMessages();

    OrderFreezePowerup freeze = new OrderFreezePowerup(2);
    freeze.setTarget(frozenPlayer);
    game.activatePowerup(freeze);
    game.drainMessages();

    OrderResult blocked =
        game.placeOrder(new Order.Buy(symbol, game.currentPrices().get(symbol)), frozenPlayer);
    assertInstanceOf(OrderResult.Rejected.class, blocked);

    OrderResult allowed =
        game.placeOrder(new Order.Buy(symbol, game.currentPrices().get(symbol)), otherPlayer);
    assertInstanceOf(OrderResult.Filled.class, allowed);
    game.drainMessages();

    game.tick();
    game.tick();
    game.drainMessages();

    OrderResult afterExpiry =
        game.placeOrder(new Order.Buy(symbol, game.currentPrices().get(symbol)), frozenPlayer);
    assertInstanceOf(OrderResult.Filled.class, afterExpiry);
  }
}
