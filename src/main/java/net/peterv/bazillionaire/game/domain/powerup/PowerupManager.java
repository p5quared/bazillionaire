package net.peterv.bazillionaire.game.domain.powerup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class PowerupManager {
  private final List<Powerup> activePowerups = new ArrayList<>();
  private final List<PowerupTrigger> triggers = new ArrayList<>();
  private final Map<PlayerId, List<Powerup>> inventory = new HashMap<>();

  public List<PowerupEffect> activate(Powerup powerup) {
    activePowerups.add(powerup);
    return powerup.onActivate();
  }

  public void collect(PlayerId recipient, Powerup powerup) {
    inventory.computeIfAbsent(recipient, k -> new ArrayList<>()).add(powerup);
  }

  public UsePowerupResult usePowerup(PlayerId playerId, String powerupName, PlayerId target) {
    return usePowerup(playerId, powerupName, 1, target, null);
  }

  public UsePowerupResult usePowerup(
      PlayerId playerId, String powerupName, int quantity, PlayerId target) {
    return usePowerup(playerId, powerupName, quantity, target, null);
  }

  public UsePowerupResult usePowerup(
      PlayerId playerId,
      String powerupName,
      int quantity,
      PlayerId targetPlayer,
      Symbol targetSymbol) {
    List<Powerup> owned = inventory.getOrDefault(playerId, Collections.emptyList());
    List<Powerup> matches =
        owned.stream()
            .filter(p -> p.name().equals(powerupName))
            .limit(Math.max(quantity, 1))
            .toList();
    if (matches.isEmpty()) {
      return new UsePowerupResult.NotOwned();
    }
    // Validate target once (all same type)
    Powerup first = matches.get(0);
    if (first.usageType() == PowerupUsageType.TARGET_PLAYER) {
      if (targetPlayer == null) {
        return new UsePowerupResult.InvalidTarget("Must select a target");
      }
      if (targetPlayer.equals(playerId)) {
        return new UsePowerupResult.InvalidTarget("Cannot target yourself");
      }
    }
    if (first.usageType() == PowerupUsageType.TARGET_SYMBOL) {
      if (targetSymbol == null) {
        return new UsePowerupResult.InvalidTarget("Must select a target symbol");
      }
    }
    List<PowerupEffect> allEffects = new ArrayList<>();
    for (Powerup match : matches) {
      if (match.usageType() == PowerupUsageType.TARGET_PLAYER) {
        match.setTarget(targetPlayer);
      }
      if (match.usageType() == PowerupUsageType.TARGET_SYMBOL) {
        match.setSymbolTarget(targetSymbol);
      }
      owned.remove(match);
      allEffects.addAll(activate(match));
      allEffects.add(
          new PowerupEffect.Emit(
              GameMessage.broadcast(new GameEvent.PowerupActivated(playerId, powerupName))));
    }
    return new UsePowerupResult.Activated(allEffects);
  }

  public List<Powerup> getInventory(PlayerId playerId) {
    return Collections.unmodifiableList(inventory.getOrDefault(playerId, Collections.emptyList()));
  }

  public void registerTrigger(PowerupTrigger trigger) {
    triggers.add(trigger);
  }

  public List<PowerupEffect> tick(GameContext context) {
    List<PowerupEffect> effects = new ArrayList<>();
    activePowerups.forEach(p -> effects.addAll(p.tick()));
    List<Powerup> expired = activePowerups.stream().filter(Powerup::isExpired).toList();
    expired.forEach(p -> effects.addAll(p.onDeactivate()));
    activePowerups.removeAll(expired);

    if (!triggers.isEmpty()) {
      for (PowerupTrigger trigger : triggers) {
        for (AwardedPowerup award : trigger.evaluate(context)) {
          collect(award.recipient(), award.powerup());
          effects.add(
              new PowerupEffect.Emit(
                  GameMessage.broadcast(
                      new GameEvent.PowerupAwarded(
                          award.recipient(),
                          award.powerup().name(),
                          award.powerup().description(),
                          award.powerup().usageType().name().toLowerCase(),
                          award.powerup().consumptionMode().name().toLowerCase()))));
        }
      }
    }
    return effects;
  }

  /**
   * Run the order through all active interceptors. Returns the first non-null result (a
   * rejection/block), or null if no interceptor cares.
   */
  public OrderResult checkInterceptors(Order order, PlayerId playerId, Ticker ticker) {
    for (Powerup powerup : activePowerups) {
      if (powerup instanceof OrderInterceptor interceptor) {
        OrderResult result = interceptor.intercept(order, playerId, ticker);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }
}
