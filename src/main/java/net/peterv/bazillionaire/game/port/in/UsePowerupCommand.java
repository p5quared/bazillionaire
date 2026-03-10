package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public record UsePowerupCommand(
    String gameId, String playerId, String powerupName, String targetPlayerId, int quantity) {
  public UsePowerupCommand(
      String gameId, String playerId, String powerupName, String targetPlayerId) {
    this(gameId, playerId, powerupName, targetPlayerId, 1);
  }

  public GameId toGameId() {
    return new GameId(gameId);
  }

  public PlayerId toPlayerId() {
    return new PlayerId(playerId);
  }

  public PlayerId toTargetPlayerId() {
    return targetPlayerId != null ? new PlayerId(targetPlayerId) : null;
  }
}
