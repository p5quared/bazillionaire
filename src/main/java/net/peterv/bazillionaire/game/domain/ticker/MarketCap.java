package net.peterv.bazillionaire.game.domain.ticker;

import java.util.Random;
import net.peterv.bazillionaire.game.domain.types.Money;

public enum MarketCap {
  STARTUP,
  MID_CAP,
  BLUE_CHIP;

  public Money initialPrice() {
    return switch (this) {
      case STARTUP -> new Money(5_00);
      case MID_CAP -> new Money(25_00);
      case BLUE_CHIP -> new Money(250_00);
    };
  }

  public static MarketCap pick(Random random) {
    double roll = random.nextDouble();
    if (roll < 0.40) return STARTUP;
    if (roll < 0.80) return MID_CAP;
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
