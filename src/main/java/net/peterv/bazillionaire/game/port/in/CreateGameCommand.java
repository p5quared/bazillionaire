package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.List;
import java.util.Random;

public record CreateGameCommand(
		String gameId,
		List<String> playerIds,
		int tickerCount,
		Money initialBalance,
		Money initialPrice,
		int gameDuration,
		Random random) {

	public GameId toGameId() {
		return new GameId(gameId);
	}

	public List<PlayerId> toPlayerIds() {
		return playerIds.stream().map(PlayerId::new).toList();
	}
}
