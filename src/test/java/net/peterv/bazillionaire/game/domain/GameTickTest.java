package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTickTest {

	private static final Money INITIAL_BALANCE = new Money(100_000_00);
	private static final Money INITIAL_PRICE = new Money(100_00);
	private static final int TICKER_COUNT = 3;
	private static final int TOTAL_DURATION = 200;
	private static final int STRATEGY_DURATION = 50;
	private static final long SEED = 42L;

	private static final PlayerId PLAYER_1 = new PlayerId("player1");

	@Test
	void tickBeforeStartProducesNoEventsAndKeepsInitialProgress() {
		Game game = createGame(List.of(PLAYER_1), TOTAL_DURATION, STRATEGY_DURATION);
		game.join(PLAYER_1);
		game.drainMessages();

		game.tick();

		assertEquals(0, game.drainMessages().size());
		assertEquals(0, game.currentTick());
		assertEquals(TOTAL_DURATION, game.ticksRemaining());
	}

	@Test
	void tickAfterStartEmitsTickerAndProgress() {
		Game game = createStartedGame(List.of(PLAYER_1), TOTAL_DURATION, STRATEGY_DURATION);

		game.tick();

		var messages = game.drainMessages();
		assertTrue(messages.stream().anyMatch(m -> m.event() instanceof GameEvent.TickerTicked));
		GameEvent.GameTickProgressed progress = findTickProgressed(messages);
		assertEquals(1, progress.tick());
		assertEquals(TOTAL_DURATION - 1, progress.ticksRemaining());
	}

	@Test
	void finalTickEmitsProgressAndFinished() {
		int shortDuration = 2;
		Game game = createStartedGame(List.of(PLAYER_1), shortDuration, 1);

		game.tick();
		game.drainMessages();

		game.tick();

		var messages = game.drainMessages();
		GameEvent.GameTickProgressed progress = findTickProgressed(messages);
		assertEquals(shortDuration, progress.tick());
		assertEquals(0, progress.ticksRemaining());
		assertTrue(messages.stream().anyMatch(m -> m.event() instanceof GameEvent.GameFinished));
	}

	private Game createGame(List<PlayerId> players, int totalDuration, int strategyDuration) {
		Game game = Game.create(players, TICKER_COUNT, INITIAL_BALANCE, INITIAL_PRICE, totalDuration, strategyDuration,
				new Random(SEED));
		game.drainMessages();
		return game;
	}

	private Game createStartedGame(List<PlayerId> players, int totalDuration, int strategyDuration) {
		Game game = createGame(players, totalDuration, strategyDuration);
		players.forEach(game::join);
		game.start();
		game.drainMessages();
		return game;
	}

	private GameEvent.GameTickProgressed findTickProgressed(List<GameMessage> messages) {
		return messages.stream()
				.map(GameMessage::event)
				.filter(GameEvent.GameTickProgressed.class::isInstance)
				.map(GameEvent.GameTickProgressed.class::cast)
				.findFirst()
				.orElseThrow();
	}
}
