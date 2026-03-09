package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.Portfolio;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashBoostPowerupTest {

	@Test
	void addsMoneyToRecipientOnActivation() {
		PlayerId player = new PlayerId("p1");
		Game game = new Game(
				Map.of(player, new Portfolio(new Money(100000))),
				Map.of(), 100);

		new CashBoostPowerup(player, new Money(50000)).onActivate(game);

		GameEvent.PlayerPortfolio portfolio = game.snapshot().players().get(player);
		assertEquals(new Money(150000), portfolio.cashBalance());
		List<GameMessage> messages = game.drainMessages();
		assertEquals(1, messages.size());
		GameEvent.PlayersState playersState = assertInstanceOf(GameEvent.PlayersState.class, messages.get(0).event());
		assertEquals(new Money(150000), playersState.players().get(player).cashBalance());
	}

	@Test
	void cashBoostCollectedIntoInventoryDuringTick() {
		PlayerId player = new PlayerId("p1");
		Game game = new Game(
				Map.of(player, new Portfolio(new Money(100000))),
				Map.of(), 60);
		game.registerTrigger(new RandomTickTrigger(1.0, new Money(50000), new java.util.Random(42)));
		game.start();
		game.drainMessages();
		game.tick();

		List<GameMessage> messages = game.drainMessages();
		boolean awarded = messages.stream()
				.map(GameMessage::event)
				.anyMatch(GameEvent.PowerupAwarded.class::isInstance);
		assertTrue(awarded);
		// Powerup is in inventory, not yet activated — cash unchanged
		assertEquals(1, game.getInventory(player).size());
		GameEvent.PlayerPortfolio portfolio = game.snapshot().players().get(player);
		assertEquals(new Money(100000), portfolio.cashBalance());
	}
}
