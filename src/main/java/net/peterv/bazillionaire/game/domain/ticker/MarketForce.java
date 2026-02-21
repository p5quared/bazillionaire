package net.peterv.bazillionaire.game.domain.ticker;

public record MarketForce(
        int volatilityMultiplier,
        int directionalAdder
) {
    public static MarketForce neutral() {
        return new MarketForce(1, 0);
    }

    public MarketForce combine(MarketForce other) {
        return new MarketForce(
                this.volatilityMultiplier * other.volatilityMultiplier,
                this.directionalAdder + other.directionalAdder
        );
    }
}
