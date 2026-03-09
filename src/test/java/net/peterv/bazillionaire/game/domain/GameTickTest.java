package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTickTest {

	@Test
	void tickBeforeStartProducesNoEventsAndKeepsInitialProgress() {
		Game game = pendingGame(PLAYER_1);
		game.join(PLAYER_1);
		game.drainMessages();

		game.tick();

		assertEquals(0, game.drainMessages().size());
		assertEquals(0, game.currentTick());
		assertEquals(TOTAL_DURATION, game.ticksRemaining());
	}

	@Test
	void tickAfterStartEmitsTickerAndProgress() {
		Game game = startedGame(PLAYER_1);

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
		Game game = startedGame(shortDuration, PLAYER_1);

		game.tick();
		game.drainMessages();

		game.tick();

		var messages = game.drainMessages();
		GameEvent.GameTickProgressed progress = findTickProgressed(messages);
		assertEquals(shortDuration, progress.tick());
		assertEquals(0, progress.ticksRemaining());
		assertTrue(messages.stream().anyMatch(m -> m.event() instanceof GameEvent.GameFinished));
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
