package net.peterv.bazillionaire.game.domain.powerup;

public sealed interface UsePowerupResult permits UsePowerupResult.Activated, UsePowerupResult.NotOwned {
	record Activated() implements UsePowerupResult {
	}

	record NotOwned() implements UsePowerupResult {
	}
}
