package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.port.in.TickCommand;
import net.peterv.bazillionaire.game.port.in.TickUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

public class TickService implements TickUseCase {
	private final GameRepository gameRepository;

	public TickService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<Void> tick(TickCommand cmd) {
		return gameRepository.withGame(cmd.toGameId(), game -> {
			game.tick();
			return new UseCaseResult<>(null, game.drainMessages());
		});
	}
}
