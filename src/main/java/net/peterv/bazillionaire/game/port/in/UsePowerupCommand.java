package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public record UsePowerupCommand(String gameId, String playerId, String powerupName) {
	public GameId toGameId() {
		return new GameId(gameId);
	}

	public PlayerId toPlayerId() {
		return new PlayerId(playerId);
	}
}
