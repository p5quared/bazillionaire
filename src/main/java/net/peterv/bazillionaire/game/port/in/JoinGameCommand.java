package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public record JoinGameCommand(String gameId, String playerId) {
	public GameId toGameId() {
		return new GameId(gameId);
	}

	public PlayerId toPlayerId() {
		return new PlayerId(playerId);
	}
}
