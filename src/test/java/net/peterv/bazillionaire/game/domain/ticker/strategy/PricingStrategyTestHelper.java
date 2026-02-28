package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTestHelper {

    static void assertKindAndExhausted(PricingStrategy strategy, StrategyKind expectedKind) {
        assertEquals(expectedKind, strategy.kind());
        assertTrue(strategy.isExhausted(), "Strategy should be exhausted");
    }

    static void assertBoundedConvergence(
            PricingStrategy strategy,
            Money startPrice,
            Money endPrice,
            int duration,
            StrategyKind expectedKind) {
        int minCents = Math.min(startPrice.cents(), endPrice.cents());
        int maxCents = Math.max(startPrice.cents(), endPrice.cents());
        for (int i = 0; i < duration - 1; i++) {
            int price = strategy.nextPrice().cents();
            assertTrue(price >= minCents && price <= maxCents,
                    "Price %d at tick %d out of range [%d, %d]".formatted(price, i + 1, minCents, maxCents));
        }
        int lastPrice = strategy.nextPrice().cents();
        assertEquals(endPrice.cents(), lastPrice, "Exhausted tick should return endPrice");
        assertKindAndExhausted(strategy, expectedKind);
    }

    static int[] drainPrices(PricingStrategy strategy, int ticks) {
        int[] prices = new int[ticks];
        for (int i = 0; i < ticks; i++) {
            prices[i] = strategy.nextPrice().cents();
        }
        return prices;
    }
}
