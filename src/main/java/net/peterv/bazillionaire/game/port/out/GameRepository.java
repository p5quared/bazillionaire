package net.peterv.bazillionaire.game.port.out;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.GameId;

public interface GameRepository {
    Game loadGame(GameId gameId);
    void saveGame(GameId gameId, Game game);
}
