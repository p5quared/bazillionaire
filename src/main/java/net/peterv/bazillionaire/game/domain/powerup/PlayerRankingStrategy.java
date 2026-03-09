package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.Comparator;

public final class PlayerRankingStrategy implements TargetingStrategy {
	private final Comparator<Integer> comparator;

	private PlayerRankingStrategy(Comparator<Integer> comparator) {
		this.comparator = comparator;
	}

	public static PlayerRankingStrategy leading() {
		return new PlayerRankingStrategy(Comparator.reverseOrder());
	}

	public static PlayerRankingStrategy trailing() {
		return new PlayerRankingStrategy(Comparator.naturalOrder());
	}

	@Override
	public PlayerId selectTarget(GameContext context) {
		PlayerId selected = null;
		int selectedBalance = 0;

		for (var entry : context.players().entrySet()) {
			int balance = entry.getValue().cashBalance().cents();
			if (selected == null || comparator.compare(balance, selectedBalance) < 0) {
				selectedBalance = balance;
				selected = entry.getKey();
			}
		}

		return selected;
	}
}
