package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class DarkPoolPowerup extends Powerup {
  private final DarkPoolTier tier;
  private final PlayerId owner;
  private Symbol targetSymbol;
  private int remainingTokens;

  public DarkPoolPowerup(DarkPoolTier tier, PlayerId owner) {
    super(tier.ticks());
    this.tier = tier;
    this.owner = owner;
    this.remainingTokens = tier.tokens();
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
    return tier.allSymbols() ? PowerupUsageType.INSTANT : PowerupUsageType.TARGET_SYMBOL;
  }

  @Override
  public ConsumptionMode consumptionMode() {
    return ConsumptionMode.SINGLE;
  }

  @Override
  public void setSymbolTarget(Symbol target) {
    this.targetSymbol = target;
  }

  @Override
  public boolean broadcastsActivation() {
    return false;
  }

  @Override
  public boolean isExpired() {
    return remainingTicks == 0 || remainingTokens <= 0;
  }

  @Override
  public List<PowerupEffect> onActivate() {
    return List.of(
        new PowerupEffect.Emit(
            GameMessage.send(
                new GameEvent.DarkPoolActivated(
                    owner, tier.displayName(), targetSymbol, remainingTokens, remainingTicks),
                owner)));
  }

  @Override
  public List<PowerupEffect> onDeactivate() {
    return List.of(
        new PowerupEffect.Emit(GameMessage.send(new GameEvent.DarkPoolExpired(owner), owner)));
  }

  public boolean appliesTo(Symbol symbol) {
    return tier.allSymbols() || symbol.equals(targetSymbol);
  }

  public boolean hasTokens() {
    return remainingTokens > 0;
  }

  public void consumeToken() {
    if (remainingTokens > 0) {
      remainingTokens--;
    }
  }

  public PlayerId owner() {
    return owner;
  }

  public int remainingTokens() {
    return remainingTokens;
  }
}
