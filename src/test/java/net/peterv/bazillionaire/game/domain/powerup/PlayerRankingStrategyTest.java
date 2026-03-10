package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRankingStrategyTest {

	@Test
	void trailingStrategySelectsPlayerWithLowestCashBalance() {
		PlayerId rich = new PlayerId("rich");
		PlayerId poor = new PlayerId("poor");

		assertEquals(poor, PlayerRankingStrategy.trailing().selectTarget(contextWithBalances(Map.of(
				rich, 5_000_00,
				poor, 500_00))));
	}

	@Test
	void leadingStrategySelectsPlayerWithHighestCashBalance() {
		PlayerId rich = new PlayerId("rich");
		PlayerId poor = new PlayerId("poor");

		assertEquals(rich, PlayerRankingStrategy.leading().selectTarget(contextWithBalances(Map.of(
				rich, 5_000_00,
				poor, 500_00))));
	}

	private GameContext contextWithBalances(Map<PlayerId, Integer> balances) {
		Map<PlayerId, GameEvent.PlayerPortfolio> players = new LinkedHashMap<>();
		balances.forEach((playerId, cents) -> players.put(playerId, new GameEvent.PlayerPortfolio(new Money(cents), Map.of())));
		return new GameContext(0, players, Map.of(), List.of());
	}
}
