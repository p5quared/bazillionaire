package net.peterv.bazillionaire.game.adapter.out;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.port.out.GameRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@ApplicationScoped
public class InMemoryGameRepository implements GameRepository {

	private final ConcurrentHashMap<GameId, Game> games = new ConcurrentHashMap<>();

	@Override
	public void saveGame(GameId gameId, Game game) {
		games.put(gameId, game);
	}

	@Override
	public <T> T withGame(GameId gameId, Function<Game, T> action) {
		Game game = games.get(gameId);
		if (game == null) {
			throw new IllegalArgumentException("Game not found: " + gameId);
		}
		synchronized (game) {
			return action.apply(game);
		}
	}
}
