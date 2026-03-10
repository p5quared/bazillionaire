package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;

import java.util.List;

public final class OrderFreezePowerup extends Powerup implements OrderInterceptor {
	private PlayerId frozenPlayer;

	public OrderFreezePowerup(int duration) {
		super(duration);
		if (duration <= 0) {
			throw new IllegalArgumentException("Freeze duration must be positive");
		}
	}

	@Override
	public void setTarget(PlayerId target) {
		this.frozenPlayer = target;
	}

	@Override
	public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) {
		if (frozenPlayer != null && playerId.equals(frozenPlayer)) {
			return new OrderResult.Rejected("Your orders are frozen!");
		}
		return null;
	}

	@Override
	public List<PowerupEffect> onActivate() {
		if (frozenPlayer == null) {
			return List.of();
		}
		return List.of(new PowerupEffect.Emit(
				GameMessage.send(new GameEvent.FreezeStarted(frozenPlayer, remainingTicks), frozenPlayer)));
	}

	@Override
	public List<PowerupEffect> onDeactivate() {
		if (frozenPlayer == null) {
			return List.of();
		}
		return List.of(new PowerupEffect.Emit(
				GameMessage.send(new GameEvent.FreezeExpired(frozenPlayer), frozenPlayer)));
	}

	@Override
	public String name() {
		return "Order Freeze";
	}

	@Override
	public String description() {
		return "Freeze a player's orders";
	}

	@Override
	public PowerupUsageType usageType() {
		return PowerupUsageType.TARGET_PLAYER;
	}

	@Override
	public ConsumptionMode consumptionMode() {
		return ConsumptionMode.SINGLE;
	}
}
