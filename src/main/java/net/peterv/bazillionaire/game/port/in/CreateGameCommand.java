package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.List;

public record CreateGameCommand(String gameId, List<String> playerIds, int tickerCount) {
	public GameId toGameId() {
		return new GameId(gameId);
	}

	public List<PlayerId> toPlayerIds() {
		return playerIds.stream().map(PlayerId::new).toList();
	}
}
