package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GameJoinTest {

	private static final PlayerId UNKNOWN = new PlayerId("unknown");

	@Test
	void singlePlayerGameJoinScenarios() {
		var game = pendingGame(PLAYER_1);
		check(game, UNKNOWN, JoinResult.InvalidJoin.class);
		check(game, PLAYER_1, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void twoPlayerGameJoinScenarios() {
		var game = pendingGame(PLAYER_1, PLAYER_2);
		check(game, PLAYER_1, JoinResult.Joined.class, GameEvent.PlayerJoined.class);
		check(game, PLAYER_1, JoinResult.AlreadyReady.class);
		check(game, PLAYER_2, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void joinAfterGameStartedReturnsGameInProgress() {
		var game = startedGame(PLAYER_1, PLAYER_2);
		check(game, PLAYER_1, JoinResult.GameInProgress.class, GameEvent.GameState.class);
	}

	@Test
	void startBroadcastsInitialGameStateAndPlayers() {
		var game = pendingGame(PLAYER_1, PLAYER_2);
		game.join(PLAYER_1);
		game.drainMessages();
		game.join(PLAYER_2);
		game.drainMessages();

		game.start();

		var messages = game.drainMessages();
		assertEquals(2, messages.size());
		assertInstanceOf(GameEvent.GameState.class, messages.get(0).event());
		assertInstanceOf(GameEvent.PlayersState.class, messages.get(1).event());
	}

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
