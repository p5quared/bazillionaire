package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.OrderInterceptor;
import net.peterv.bazillionaire.game.domain.powerup.OrderFreezePowerup;
import net.peterv.bazillionaire.game.domain.powerup.Powerup;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GamePowerupTest {

    private static final Money INITIAL_BALANCE = new Money(100_000_00);
    private static final Money INITIAL_PRICE = new Money(100_00);
    private static final int TOTAL_DURATION = 200;
    private static final long SEED = 42L;
    private static final PlayerId PLAYER_1 = new PlayerId("player1");

    private Game createReadyGame() {
        Game game = Game.create(List.of(PLAYER_1), 3, INITIAL_BALANCE, INITIAL_PRICE, TOTAL_DURATION,
                new Random(SEED));
        game.drainMessages();
        game.join(PLAYER_1);
        game.start();
        game.drainMessages();
        return game;
    }

    private Symbol anySymbol(Game game) {
        return game.currentPrices().keySet().iterator().next();
    }

    static class BlockingInterceptor extends Powerup implements OrderInterceptor {
        BlockingInterceptor() { super(5); }

        @Override
        public String name() { return "blocking-interceptor"; }

        @Override
        public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) {
            return new OrderResult.Rejected("blocked by powerup");
        }
    }

    static class TickCountingPowerup extends Powerup {
        int onTickCount = 0;

        TickCountingPowerup(int duration) { super(duration); }

        @Override
        public String name() { return "tick-counting"; }

        @Override
        public void onTick(Game game) { onTickCount++; }
    }

    @Test
    void blockingInterceptorPreventsOrderFill() {
        Game game = createReadyGame();
        Symbol symbol = anySymbol(game);

        game.activatePowerup(new BlockingInterceptor());

        OrderResult result = game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
        assertInstanceOf(OrderResult.Rejected.class, result);
        // No fill events should be emitted
        assertTrue(game.drainMessages().isEmpty());
    }

    @Test
    void tickCountingPowerupReceivesOnTickPerGameTick() {
        Game game = createReadyGame();
        var powerup = new TickCountingPowerup(10);
        game.activatePowerup(powerup);

        game.tick();
        game.tick();
        game.tick();

        assertEquals(3, powerup.onTickCount);
    }

    @Test
    void noActivePowerupsDoesNotAffectNormalOrderFlow() {
        Game game = createReadyGame();
        Symbol symbol = anySymbol(game);

        OrderResult result = game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
        assertInstanceOf(OrderResult.Filled.class, result);
    }

    @Test
    void noActivePowerupsDoesNotAffectNormalTickFlow() {
        Game game = createReadyGame();

        game.tick();

        assertEquals(1, game.currentTick());
    }

    @Test
    void orderFreezeBlocksOrdersUntilExpiry() {
        PlayerId frozenPlayer = new PlayerId("player1");
        PlayerId otherPlayer = new PlayerId("player2");
        Symbol symbol = new Symbol("ABC");
        Game game = new Game(
                Map.of(
                        frozenPlayer, new Portfolio(INITIAL_BALANCE),
                        otherPlayer, new Portfolio(INITIAL_BALANCE)),
                Map.of(symbol, new Ticker(INITIAL_PRICE, new Random(SEED))),
                TOTAL_DURATION);
        game.start();
        game.drainMessages();

        game.activatePowerup(new OrderFreezePowerup(frozenPlayer, 2));
        game.drainMessages();

        OrderResult blocked = game.placeOrder(
                new Order.Buy(symbol, game.currentPrices().get(symbol)),
                frozenPlayer);
        assertInstanceOf(OrderResult.Rejected.class, blocked);

        OrderResult allowed = game.placeOrder(
                new Order.Buy(symbol, game.currentPrices().get(symbol)),
                otherPlayer);
        assertInstanceOf(OrderResult.Filled.class, allowed);
        game.drainMessages();

        game.tick();
        game.tick();
        game.drainMessages();

        OrderResult afterExpiry = game.placeOrder(
                new Order.Buy(symbol, game.currentPrices().get(symbol)),
                frozenPlayer);
        assertInstanceOf(OrderResult.Filled.class, afterExpiry);
    }
}
