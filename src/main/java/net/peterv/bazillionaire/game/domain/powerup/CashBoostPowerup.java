package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public class CashBoostPowerup extends Powerup {
	private final PlayerId recipient;
	private final Money amount;

	public CashBoostPowerup(PlayerId recipient, Money amount) {
		super(0);
		this.recipient = recipient;
		this.amount = amount;
	}

	@Override
	public void onActivate(Game game) {
		game.addCashToPlayer(recipient, amount);
	}

	@Override
	public String name() {
		return "Cash Boost";
	}
}
