package net.peterv.bazillionaire.game.domain.ticker.strategy;

import net.peterv.bazillionaire.game.domain.types.Money;

public class CyclePricingStrategyFactory implements PricingStrategyFactory {

    private final Money initialPrice;
    private final int stepCents;
    private final int stepsPerPhase;
    private final int numCycles;

    public CyclePricingStrategyFactory(Money initialPrice, int stepCents, int stepsPerPhase, int numCycles) {
        this.initialPrice = initialPrice;
        this.stepCents = stepCents;
        this.stepsPerPhase = stepsPerPhase;
        this.numCycles = numCycles;
    }

    @Override
    public PricingStrategy nextStrategy() {
        return new CyclePricingStrategy(initialPrice, stepCents, stepsPerPhase, numCycles);
    }
}
