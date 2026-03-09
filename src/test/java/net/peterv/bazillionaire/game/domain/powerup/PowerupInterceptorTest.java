package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PowerupInterceptorTest {

    static class PassThroughInterceptor extends Powerup implements OrderInterceptor {
        PassThroughInterceptor() { super(5); }

        @Override
        public String name() { return "pass-through"; }

        @Override
        public String description() { return "test"; }

        @Override
        public PowerupUsageType usageType() { return PowerupUsageType.INSTANT; }

        @Override
        public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) { return null; }
    }

    static class BlockingInterceptor extends Powerup implements OrderInterceptor {
        final OrderResult result;

        BlockingInterceptor(OrderResult result) {
            super(5);
            this.result = result;
        }

        @Override
        public String name() { return "blocking"; }

        @Override
        public String description() { return "test"; }

        @Override
        public PowerupUsageType usageType() { return PowerupUsageType.INSTANT; }

        @Override
        public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) { return result; }
    }

    static class NeverReachedInterceptor extends Powerup implements OrderInterceptor {
        NeverReachedInterceptor() { super(5); }

        @Override
        public String name() { return "never-reached"; }

        @Override
        public String description() { return "test"; }

        @Override
        public PowerupUsageType usageType() { return PowerupUsageType.INSTANT; }

        @Override
        public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) {
            throw new AssertionError("should not be called");
        }
    }

    @Test
    void returnsNullWhenNoPowerupsActive() {
        var manager = new PowerupManager();
        assertNull(manager.checkInterceptors(null, new PlayerId("p1"), null));
    }

    @Test
    void returnsNullWhenNoPowerupImplementsInterceptor() {
        var manager = new PowerupManager();
        manager.activate(new TrackingPowerup(5));
        assertNull(manager.checkInterceptors(null, new PlayerId("p1"), null));
    }

    @Test
    void shortCircuitsOnFirstNonNullResult() {
        var manager = new PowerupManager();
        OrderResult blocked = new OrderResult.Rejected("blocked by powerup");

        manager.activate(new PassThroughInterceptor());    // returns null — pass through
        manager.activate(new BlockingInterceptor(blocked)); // returns result — short-circuit
        manager.activate(new NeverReachedInterceptor());   // must never be called

        assertSame(blocked, manager.checkInterceptors(null, new PlayerId("p1"), null));
    }
}
