package net.peterv.bazillionaire.game.domain.types;

public record Money(int cents) {
    public Money {
        if (cents < 0) throw new IllegalArgumentException("Money cannot be negative");
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.cents >= other.cents;
    }

    public Money add(Money other) {
        return new Money(this.cents + other.cents);
    }

    public Money subtract(Money other) {
        return new Money(this.cents - other.cents);
    }
}
