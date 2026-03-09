package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchUpFreezeTriggerTest {

	@Test
	void awardsFreezeToTrailingPlayerAndTargetsLeader() {
		PlayerId leader = new PlayerId("leader");
		PlayerId trailer = new PlayerId("trailer");
		CatchUpFreezeTrigger trigger = new CatchUpFreezeTrigger(1.0, 3, new Random(42));

		List<AwardedPowerup> awards = trigger.evaluate(contextWithBalances(Map.of(
				leader, 5_000_00,
				trailer, 500_00)));

		assertEquals(1, awards.size());
		assertEquals(trailer, awards.get(0).recipient());
		assertInstanceOf(OrderFreezePowerup.class, awards.get(0).powerup());

		OrderFreezePowerup awardedPowerup = (OrderFreezePowerup) awards.get(0).powerup();
		Order order = new Order.Buy(new Symbol("ABC"), new Money(100_00));
		assertInstanceOf(OrderResult.Rejected.class, awardedPowerup.intercept(order, leader, null));
		assertNull(awardedPowerup.intercept(order, trailer, null));
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

	@Test
	void awardsNothingWhenLeaderAndTrailerCollapseToSamePlayer() {
		PlayerId p1 = new PlayerId("p1");
		PlayerId p2 = new PlayerId("p2");
		CatchUpFreezeTrigger trigger = new CatchUpFreezeTrigger(1.0, 3, new Random(42));

		assertTrue(trigger.evaluate(contextWithBalances(Map.of(
				p1, 1_000_00,
				p2, 1_000_00))).isEmpty());
	}

	private GameContext contextWithBalances(Map<PlayerId, Integer> balances) {
		Map<PlayerId, GameEvent.PlayerPortfolio> players = new LinkedHashMap<>();
		balances
				.forEach((playerId, cents) -> players.put(playerId, new GameEvent.PlayerPortfolio(new Money(cents), Map.of())));
		return new GameContext(0, players, Map.of(), List.of());
	}
}
