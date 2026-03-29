package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public final class GameTestFixtures {

  public static Game pendingGame(PlayerId... players) {
    Game game =
        GameFactory.create(
            List.of(players), TICKER_COUNT, INITIAL_BALANCE, TOTAL_DURATION, new Random(SEED));
    game.drainMessages();
    return game;
  }

  public static Game startedGame(PlayerId... players) {
    return startedGame(TOTAL_DURATION, players);
  }

  public static Game startedGame(int duration, PlayerId... players) {
    Game game =
        GameFactory.create(
            List.of(players), TICKER_COUNT, INITIAL_BALANCE, duration, new Random(SEED));
    game.drainMessages();
    for (PlayerId player : players) {
      game.join(player);
    }
    game.start();
    game.drainMessages();
    return game;
  }

  public static Symbol anySymbol(Game game) {
    return game.currentPrices().keySet().iterator().next();
  }

  private GameTestFixtures() {}
}
