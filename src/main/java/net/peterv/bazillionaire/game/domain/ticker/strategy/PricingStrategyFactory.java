package net.peterv.bazillionaire.game.domain.ticker.strategy;

public interface PricingStrategyFactory {
    PricingStrategy nextStrategy();
}
