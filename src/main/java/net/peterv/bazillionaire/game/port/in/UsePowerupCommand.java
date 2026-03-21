package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public record UsePowerupCommand(
    String gameId,
    String playerId,
    String powerupName,
    String targetPlayerId,
    String targetSymbol,
    int quantity) {
  public UsePowerupCommand(
      String gameId, String playerId, String powerupName, String targetPlayerId) {
    this(gameId, playerId, powerupName, targetPlayerId, null, 1);
  }

  public UsePowerupCommand(
      String gameId, String playerId, String powerupName, String targetPlayerId, int quantity) {
    this(gameId, playerId, powerupName, targetPlayerId, null, quantity);
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

  public Symbol toTargetSymbol() {
    return targetSymbol != null ? new Symbol(targetSymbol) : null;
  }
}
