package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

public final class LeadingPlayerStrategy implements TargetingStrategy {
	@Override
	public PlayerId selectTarget(GameContext context) {
		PlayerId leadingPlayer = null;
		int highestBalance = Integer.MIN_VALUE;

		for (var entry : context.players().entrySet()) {
			int balance = entry.getValue().cashBalance().cents();
			if (leadingPlayer == null || balance > highestBalance) {
				highestBalance = balance;
				leadingPlayer = entry.getKey();
			}
		}

		return leadingPlayer;
	}
}
