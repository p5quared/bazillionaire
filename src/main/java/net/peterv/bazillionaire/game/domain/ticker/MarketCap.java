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

  public BubbleTracker createBubbleTracker() {
    return switch (this) {
      case STARTUP -> new BubbleTracker(20, 8);
      case MID_CAP -> new BubbleTracker(30, 15);
      case BLUE_CHIP -> new BubbleTracker(50, 30);
    };
  }
}
