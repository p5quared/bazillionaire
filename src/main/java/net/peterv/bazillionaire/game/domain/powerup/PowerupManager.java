package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.ArrayList;
import java.util.List;

public class PowerupManager {
	private final List<Powerup> activePowerups = new ArrayList<>();
	private final List<PowerupTrigger> triggers = new ArrayList<>();

	public void activate(Powerup powerup, Game game) {
		activePowerups.add(powerup);
		powerup.onActivate(game);
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
					activate(award.powerup(), game);
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
	public OrderResult checkInterceptors(Order order, Ticker ticker) {
		for (Powerup powerup : activePowerups) {
			if (powerup instanceof OrderInterceptor interceptor) {
				OrderResult result = interceptor.intercept(order, ticker);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
}
