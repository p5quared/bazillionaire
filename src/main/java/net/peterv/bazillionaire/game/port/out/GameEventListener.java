package net.peterv.bazillionaire.game.port.out;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;

public interface GameEventListener {

  /** Called with every batch of messages after each use case execution. */
  default void onGameEvents(GameId gameId, List<GameMessage> messages) {}

  /** Called once at game end with a snapshot of the final state. */
  default void onGameFinished(GameId gameId, GameFinishedSnapshot snapshot) {}
}
