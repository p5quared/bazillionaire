package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.Portfolio;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	}

	@Test
	void cashBoostAppliedDuringTick() {
		PlayerId player = new PlayerId("p1");
		Game game = new Game(
				Map.of(player, new Portfolio(new Money(100000))),
				Map.of(), 60);
		game.registerTrigger(new RandomTickTrigger(1.0, new Money(50000), new java.util.Random(42)));
		game.start();
		game.tick();

		boolean awarded = game.drainMessages().stream()
				.map(GameMessage::event)
				.anyMatch(e -> e instanceof GameEvent.PowerupAwarded);
		assertTrue(awarded);
	}
}
