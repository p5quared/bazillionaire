package net.peterv.bazillionaire.game.port.out;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.GameId;

import java.util.function.Function;

public interface GameRepository {
	<T> T withGame(GameId gameId, Function<Game, T> action);

	void saveGame(GameId gameId, Game game);

	void removeGame(GameId gameId);
}
