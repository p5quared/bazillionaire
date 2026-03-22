package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public class CashBoostPowerup extends Powerup {
  private final PlayerId recipient;
  private final CashBoostTier tier;

  public CashBoostPowerup(PlayerId recipient, CashBoostTier tier) {
    super(0);
    this.recipient = recipient;
    this.tier = tier;
  }

  @Override
  public List<PowerupEffect> onActivate() {
    return List.of(new PowerupEffect.AddCash(recipient, tier.amount()));
  }

  @Override
  public String name() {
    return tier.displayName();
  }

  @Override
  public String description() {
    return tier.description();
  }

  @Override
  public PowerupUsageType usageType() {
    return PowerupUsageType.INSTANT;
  }

  @Override
  public ConsumptionMode consumptionMode() {
    return ConsumptionMode.ALL;
  }
}
