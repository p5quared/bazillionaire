package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.List;

public class CashBoostPowerup extends Powerup {
	private final PlayerId recipient;
	private final Money amount;

	public CashBoostPowerup(PlayerId recipient, Money amount) {
		super(0);
		this.recipient = recipient;
		this.amount = amount;
	}

	@Override
	public List<PowerupEffect> onActivate() {
		return List.of(new PowerupEffect.AddCash(recipient, amount));
	}

	@Override
	public String name() {
		return "Cash Boost";
	}

	@Override
	public String description() {
		return "Instant cash injection";
	}

	@Override
	public PowerupUsageType usageType() {
		return PowerupUsageType.INSTANT;
	}
}
