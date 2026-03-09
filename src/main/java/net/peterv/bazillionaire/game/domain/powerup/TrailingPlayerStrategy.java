package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

public final class TrailingPlayerStrategy implements TargetingStrategy {
	@Override
	public PlayerId selectTarget(GameContext context) {
		PlayerId trailingPlayer = null;
		int lowestBalance = Integer.MAX_VALUE;

		for (var entry : context.players().entrySet()) {
			int balance = entry.getValue().cashBalance().cents();
			if (trailingPlayer == null || balance < lowestBalance) {
				lowestBalance = balance;
				trailingPlayer = entry.getKey();
			}
		}

		return trailingPlayer;
	}
}
