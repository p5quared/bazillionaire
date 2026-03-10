package net.peterv.bazillionaire.game.domain.powerup;

public enum DividendTier {
  TIER_1("Tier 1", 5, 300, 500),
  TIER_2("Tier 2", 10, 100, 750),
  TIER_3("Tier 3", 15, 0, 1000);

  private final String displayName;
  private final int requiredShares;
  private final int requiredHoldTicks;
  private final int yieldBasisPoints;

  DividendTier(
      String displayName, int requiredShares, int requiredHoldTicks, int yieldBasisPoints) {
    this.displayName = displayName;
    this.requiredShares = requiredShares;
    this.requiredHoldTicks = requiredHoldTicks;
    this.yieldBasisPoints = yieldBasisPoints;
  }

  public String displayName() {
    return displayName;
  }

  public int requiredShares() {
    return requiredShares;
  }

  public int requiredHoldTicks() {
    return requiredHoldTicks;
  }

  public int yieldBasisPoints() {
    return yieldBasisPoints;
  }

  public static DividendTier highestQualifying(int shares, int holdDuration) {
    DividendTier[] tiers = values();
    for (int i = tiers.length - 1; i >= 0; i--) {
      DividendTier tier = tiers[i];
      if (shares >= tier.requiredShares && holdDuration >= tier.requiredHoldTicks) {
        return tier;
      }
    }
    return null;
  }
}
