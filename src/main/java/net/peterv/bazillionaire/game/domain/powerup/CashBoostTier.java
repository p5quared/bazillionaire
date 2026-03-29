package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;

public enum CashBoostTier {
  MINOR("Cash Boost", "Instant cash injection", new Money(100_00)),
  MAJOR("Cash Boost (Major)", "Large cash injection", new Money(300_00));

  private final String displayName;
  private final String description;
  private final Money amount;

  CashBoostTier(String displayName, String description, Money amount) {
    this.displayName = displayName;
    this.description = description;
    this.amount = amount;
  }

  public String displayName() {
    return displayName;
  }

  public String description() {
    return description;
  }

  public Money amount() {
    return amount;
  }
}
