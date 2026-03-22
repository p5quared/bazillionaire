package net.peterv.bazillionaire.game.domain.ticker;

import java.util.Random;

public enum MarketCap {
  STARTUP,
  MID_CAP,
  BLUE_CHIP;

  public static MarketCap pick(Random random) {
    double roll = random.nextDouble();
    if (roll < 0.60) return STARTUP;
    if (roll < 0.90) return MID_CAP;
    return BLUE_CHIP;
  }
}
