package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerupManager {
	private final List<Powerup> activePowerups = new ArrayList<>();
	private final List<PowerupTrigger> triggers = new ArrayList<>();
	private final Map<PlayerId, List<Powerup>> inventory = new HashMap<>();

	public void activate(Powerup powerup, Game game) {
		activePowerups.add(powerup);
		powerup.onActivate(game);
	}

	public void collect(PlayerId recipient, Powerup powerup) {
		inventory.computeIfAbsent(recipient, k -> new ArrayList<>()).add(powerup);
	}

	public UsePowerupResult usePowerup(PlayerId playerId, String powerupName, Game game) {
		List<Powerup> owned = inventory.getOrDefault(playerId, Collections.emptyList());
		Powerup match = owned.stream().filter(p -> p.name().equals(powerupName)).findFirst().orElse(null);
		if (match == null) {
			return new UsePowerupResult.NotOwned();
		}
		owned.remove(match);
		activate(match, game);
		game.emit(GameMessage.broadcast(new GameEvent.PowerupActivated(playerId, powerupName)));
		return new UsePowerupResult.Activated();
	}

	public List<Powerup> getInventory(PlayerId playerId) {
		return Collections.unmodifiableList(inventory.getOrDefault(playerId, Collections.emptyList()));
	}

	public void registerTrigger(PowerupTrigger trigger) {
		triggers.add(trigger);
	}

	public void tick(Game game) {
		activePowerups.forEach(p -> p.tick(game));
		List<Powerup> expired = activePowerups.stream().filter(Powerup::isExpired).toList();
		expired.forEach(p -> p.onDeactivate(game));
		activePowerups.removeAll(expired);

		if (!triggers.isEmpty()) {
			GameContext context = game.snapshot();
			for (PowerupTrigger trigger : triggers) {
				for (AwardedPowerup award : trigger.evaluate(context)) {
					collect(award.recipient(), award.powerup());
					game.emit(GameMessage.broadcast(
							new GameEvent.PowerupAwarded(award.recipient(), award.powerup().name())));
				}
			}
		}
	}

	/**
	 * Run the order through all active interceptors.
	 * Returns the first non-null result (a rejection/block),
	 * or null if no interceptor cares.
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
