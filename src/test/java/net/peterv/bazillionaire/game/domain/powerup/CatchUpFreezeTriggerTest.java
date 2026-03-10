package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchUpFreezeTriggerTest {

	@Test
	void awardsFreezeToTrailingPlayer() {
		PlayerId leader = new PlayerId("leader");
		PlayerId trailer = new PlayerId("trailer");
		CatchUpFreezeTrigger trigger = new CatchUpFreezeTrigger(1.0, 3, new Random(42));

		List<AwardedPowerup> awards = trigger.evaluate(contextWithBalances(Map.of(
				leader, 5_000_00,
				trailer, 500_00)));

		assertEquals(1, awards.size());
		assertEquals(trailer, awards.get(0).recipient());
		assertInstanceOf(OrderFreezePowerup.class, awards.get(0).powerup());
		assertEquals(PowerupUsageType.TARGET_PLAYER, awards.get(0).powerup().usageType());
	}

	@Test
	void awardsNothingWhenProbabilityIsZero() {
		CatchUpFreezeTrigger trigger = new CatchUpFreezeTrigger(0.0, 3, new Random(42));
		assertTrue(trigger.evaluate(contextWithBalances(Map.of(
				new PlayerId("leader"), 5_000_00,
				new PlayerId("trailer"), 500_00))).isEmpty());
	}

	@Test
	void awardsNothingForSinglePlayerGame() {
		CatchUpFreezeTrigger trigger = new CatchUpFreezeTrigger(1.0, 3, new Random(42));
		assertTrue(trigger.evaluate(contextWithBalances(Map.of(
				new PlayerId("solo"), 1_000_00))).isEmpty());
	}

	private GameContext contextWithBalances(Map<PlayerId, Integer> balances) {
		Map<PlayerId, GameEvent.PlayerPortfolio> players = new LinkedHashMap<>();
		balances
				.forEach((playerId, cents) -> players.put(playerId, new GameEvent.PlayerPortfolio(new Money(cents), Map.of())));
		return new GameContext(0, players, Map.of(), List.of());
	}
}
