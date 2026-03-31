package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;

@ApplicationScoped
public class PowerupStatsListener implements GameEventListener {

  @Inject PlayerPowerupStatsService powerupStatsService;

  private final ConcurrentHashMap<GameId, ConcurrentHashMap<PlayerId, PowerupAccumulator>>
      activeGames = new ConcurrentHashMap<>();

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      switch (message.event()) {
        case GameEvent.PowerupAwarded awarded ->
            accumulate(gameId, awarded.recipient()).addReceived();
        case GameEvent.PowerupActivated activated -> accumulate(gameId, activated.user()).addUsed();
        case GameEvent.FreezeStarted frozen ->
            accumulate(gameId, frozen.frozenPlayer()).addFrozen();
        case GameEvent.DarkPoolActivated darkPool ->
            accumulate(gameId, darkPool.player()).addDarkPool();
        case GameEvent.GameFinished finished -> persistAndCleanup(gameId);
        default -> {}
      }
    }
  }

  private PowerupAccumulator accumulate(GameId gameId, PlayerId playerId) {
    return activeGames
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(playerId, k -> new PowerupAccumulator());
  }

  private void persistAndCleanup(GameId gameId) {
    var players = activeGames.remove(gameId);
    if (players == null) return;
    for (var entry : players.entrySet()) {
      var acc = entry.getValue();
      powerupStatsService.recordPowerups(
          entry.getKey().value(),
          gameId.value(),
          acc.received(),
          acc.used(),
          acc.frozen(),
          acc.darkPools());
    }
  }

  private static class PowerupAccumulator {
    private int received;
    private int used;
    private int frozen;
    private int darkPools;

    void addReceived() {
      received++;
    }

    void addUsed() {
      used++;
    }

    void addFrozen() {
      frozen++;
    }

    void addDarkPool() {
      darkPools++;
    }

    int received() {
      return received;
    }

    int used() {
      return used;
    }

    int frozen() {
      return frozen;
    }

    int darkPools() {
      return darkPools;
    }
  }
}
