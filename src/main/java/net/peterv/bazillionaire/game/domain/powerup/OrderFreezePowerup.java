package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

public final class OrderFreezePowerup extends Powerup implements OrderInterceptor {
	private final PlayerId frozenPlayer;

	public OrderFreezePowerup(PlayerId frozenPlayer, int duration) {
		super(duration);
		if (duration <= 0) {
			throw new IllegalArgumentException("Freeze duration must be positive");
		}
		this.frozenPlayer = frozenPlayer;
	}

	@Override
	public OrderResult intercept(Order order, PlayerId playerId, Ticker ticker) {
		if (playerId.equals(frozenPlayer)) {
			return new OrderResult.Rejected("Your orders are frozen!");
		}
		return null;
	}

	@Override
	public void onActivate(Game game) {
		game.emit(GameMessage.send(new GameEvent.FreezeStarted(frozenPlayer, remainingTicks), frozenPlayer.value()));
	}

	@Override
	public void onDeactivate(Game game) {
		game.emit(GameMessage.send(new GameEvent.FreezeExpired(frozenPlayer), frozenPlayer.value()));
	}

	@Override
	public String name() {
		return "Order Freeze";
	}
}
