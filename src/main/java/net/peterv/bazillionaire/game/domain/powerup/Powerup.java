package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public abstract class Powerup {
  public abstract String name();

  public abstract String description();

  public abstract PowerupUsageType usageType();

  public abstract ConsumptionMode consumptionMode();

  protected int remainingTicks;

  protected Powerup(int duration) {
    this.remainingTicks = duration;
  }

  public void setTarget(PlayerId target) {}

  public List<PowerupEffect> onActivate() {
    return List.of();
  }

  public List<PowerupEffect> onTick() {
    return List.of();
  }

  public List<PowerupEffect> onDeactivate() {
    return List.of();
  }

  public boolean isExpired() {
    return remainingTicks == 0;
  }

  public List<PowerupEffect> tick() {
    if (remainingTicks > 0) {
      remainingTicks--;
    }
    if (!isExpired()) {
      return onTick();
    }
    return List.of();
  }
}
