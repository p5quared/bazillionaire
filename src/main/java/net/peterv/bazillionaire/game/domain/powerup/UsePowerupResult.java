package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;

public sealed interface UsePowerupResult permits UsePowerupResult.Activated, UsePowerupResult.NotOwned {
	record Activated(List<PowerupEffect> effects) implements UsePowerupResult {
	}

	record NotOwned() implements UsePowerupResult {
	}
}
