package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;

public record TickCommand(String gameId) {
	public GameId toGameId() {
		return new GameId(gameId);
	}
}
