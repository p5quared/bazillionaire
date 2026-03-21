package net.peterv.bazillionaire.game.adapter.out;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;
import net.peterv.bazillionaire.game.port.out.GameFinishedSnapshot;

@ApplicationScoped
public class GameEventDispatcher {

  @Inject Instance<GameEventListener> listeners;

  public void dispatch(GameId gameId, List<GameMessage> messages) {
    for (GameEventListener listener : listeners) {
      try {
        listener.onGameEvents(gameId, messages);
      } catch (Exception e) {
        Log.errorf(
            e, "Listener %s failed for game %s", listener.getClass().getSimpleName(), gameId);
      }
    }
  }

  public void dispatchFinished(GameId gameId, GameFinishedSnapshot snapshot) {
    for (GameEventListener listener : listeners) {
      try {
        listener.onGameFinished(gameId, snapshot);
      } catch (Exception e) {
        Log.errorf(
            e,
            "Listener %s failed on game finished for game %s",
            listener.getClass().getSimpleName(),
            gameId);
      }
    }
  }
}
