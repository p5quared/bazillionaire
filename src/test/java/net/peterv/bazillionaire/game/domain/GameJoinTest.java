package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameJoinTest {

	private static final Money INITIAL_BALANCE = new Money(100_000_00);
	private static final Money INITIAL_PRICE = new Money(100_00);
	private static final int TOTAL_DURATION = 200;
	private static final int STRATEGY_DURATION = 50;
	private static final long SEED = 42L;

	private static final PlayerId PLAYER_1 = new PlayerId("player1");
	private static final PlayerId PLAYER_2 = new PlayerId("player2");
	private static final PlayerId UNKNOWN = new PlayerId("unknown");

	private Game createGame(List<PlayerId> players) {
		Game game = Game.create(players, 3, INITIAL_BALANCE, INITIAL_PRICE, TOTAL_DURATION, STRATEGY_DURATION,
				new Random(SEED));
		game.drainMessages();
		return game;
	}

	private Game createStartedGame(List<PlayerId> players) {
		Game game = createGame(players);
		players.forEach(game::join);
		game.start();
		game.drainMessages();
		return game;
	}

	@Test
	void singlePlayerGameJoinScenarios() {
		var game = createGame(List.of(PLAYER_1));
		check(game, UNKNOWN, JoinResult.InvalidJoin.class);
		check(game, PLAYER_1, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void twoPlayerGameJoinScenarios() {
		var game = createGame(List.of(PLAYER_1, PLAYER_2));
		check(game, PLAYER_1, JoinResult.Joined.class, GameEvent.PlayerJoined.class);
		check(game, PLAYER_1, JoinResult.AlreadyReady.class);
		check(game, PLAYER_2, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void joinAfterGameStartedReturnsGameInProgress() {
		var game = createStartedGame(List.of(PLAYER_1, PLAYER_2));
		check(game, PLAYER_1, JoinResult.GameInProgress.class, GameEvent.GameState.class);
	}

	@Test
	void startTransitionsGameToReady() {
		var game = createGame(List.of(PLAYER_1));
		game.join(PLAYER_1);
		game.drainMessages();
		// Before start(), tick should produce no events
		game.tick();
		assertEquals(0, game.drainMessages().size());
		// After start(), tick produces ticker events
		game.start();
		game.tick();
		var messages = game.drainMessages();
		assertTrue(messages.stream().anyMatch(m -> m.event() instanceof GameEvent.TickerTicked));
	}

	/**
	 * Calls {@link Game#join(PlayerId)} 
	 * and asserts that the result against {@code expectedResult} 
	 * and asserts drained messages match {@code expectedEvents} in order.
	 */
	@SafeVarargs
	private void check(Game game, PlayerId player,
			Class<? extends JoinResult> expectedResult,
			Class<? extends GameEvent>... expectedEvents) {
		var result = game.join(player);
		assertInstanceOf(expectedResult, result);
		var messages = game.drainMessages();
		assertEquals(expectedEvents.length, messages.size());
		for (int i = 0; i < expectedEvents.length; i++) {
			assertInstanceOf(expectedEvents[i], messages.get(i).event());
		}
	}
}
