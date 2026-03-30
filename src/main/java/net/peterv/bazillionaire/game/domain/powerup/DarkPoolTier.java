package net.peterv.bazillionaire.game.domain.powerup;

public enum DarkPoolTier {
  STANDARD(12, 30, false, "Dark Pool", "Trade one ticker without liquidity or bubble limits"),
  PREMIUM(
      20, 30, true, "Premium Dark Pool", "Trade all tickers without liquidity or bubble limits");

  private final int tokens;
  private final int ticks;
  private final boolean allSymbols;
  private final String displayName;
  private final String description;

  DarkPoolTier(int tokens, int ticks, boolean allSymbols, String displayName, String description) {
    this.tokens = tokens;
    this.ticks = ticks;
    this.allSymbols = allSymbols;
    this.displayName = displayName;
    this.description = description;
  }

  public int tokens() {
    return tokens;
  }

  public int ticks() {
    return ticks;
  }

  public boolean allSymbols() {
    return allSymbols;
  }

  public String displayName() {
    return displayName;
  }

  public String description() {
    return description;
  }
}
