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

	private void readyPlayer(Game game, PlayerId player) {
		game.join(player);
		game.drainMessages();
	}

	@Test
	void unknownPlayerReturnsInvalidJoin() {
		var game = createGame(List.of(PLAYER_1));
		assertJoin(game, UNKNOWN, JoinResult.InvalidJoin.class);
	}

	@Test
	void firstPlayerJoinsSuccessfully() {
		var game = createGame(List.of(PLAYER_1, PLAYER_2));
		assertJoin(game, PLAYER_1, JoinResult.Joined.class, GameEvent.PlayerJoined.class);
	}

	@Test
	void alreadyReadyPlayerReturnsAlreadyReady() {
		var game = createGame(List.of(PLAYER_1, PLAYER_2));
		readyPlayer(game, PLAYER_1);
		assertJoin(game, PLAYER_1, JoinResult.AlreadyReady.class);
	}

	@Test
	void lastPlayerJoinReturnsAllReady() {
		var game = createGame(List.of(PLAYER_1, PLAYER_2));
		readyPlayer(game, PLAYER_1);
		assertJoin(game, PLAYER_2, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void singlePlayerGameAllReadyOnFirstJoin() {
		var game = createGame(List.of(PLAYER_1));
		assertJoin(game, PLAYER_1, JoinResult.AllReady.class,
				GameEvent.PlayerJoined.class, GameEvent.AllPlayersReady.class);
	}

	@Test
	void joinAfterAllReadyReturnsGameInProgress() {
		var game = createGame(List.of(PLAYER_1, PLAYER_2));
		readyPlayer(game, PLAYER_1);
		readyPlayer(game, PLAYER_2);
		assertJoin(game, PLAYER_1, JoinResult.GameInProgress.class, GameEvent.GameState.class);
	}

	@SafeVarargs
	private void assertJoin(Game game, PlayerId player,
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
