package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.List;

public class DividendPowerup extends Powerup {
	private final PlayerId recipient;
	private final Money payoutAmount;
	private final Symbol symbol;
	private final String tierName;

	public DividendPowerup(PlayerId recipient, Money payoutAmount, Symbol symbol, String tierName) {
		super(0);
		this.recipient = recipient;
		this.payoutAmount = payoutAmount;
		this.symbol = symbol;
		this.tierName = tierName;
	}

	@Override
	public List<PowerupEffect> onActivate() {
		return List.of(
				new PowerupEffect.AddCash(recipient, payoutAmount),
				new PowerupEffect.Emit(GameMessage.broadcast(
						new GameEvent.DividendPaid(recipient, symbol, payoutAmount, tierName))));
	}

	@Override
	public String name() {
		return "Dividend:" + symbol.value();
	}

	public PlayerId recipient() {
		return recipient;
	}

	public Money payoutAmount() {
		return payoutAmount;
	}

	public Symbol symbol() {
		return symbol;
	}

	public String tierName() {
		return tierName;
	}
}
