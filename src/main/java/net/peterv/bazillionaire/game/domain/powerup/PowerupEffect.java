package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameMessage;

public sealed interface PowerupEffect {
	record Emit(GameMessage message) implements PowerupEffect {
	}

	record AddCash(PlayerId player, Money amount) implements PowerupEffect {
	}
}
