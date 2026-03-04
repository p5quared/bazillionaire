package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

@ApplicationScoped
public class CreateGameService implements CreateGameUseCase {
	private final GameRepository gameRepository;

	public CreateGameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<Void> createGame(CreateGameCommand cmd) {
		Game game = Game.create(
				cmd.toPlayerIds(),
				cmd.tickerCount(),
				cmd.initialBalance(),
				cmd.initialPrice(),
				cmd.gameDuration(),
				cmd.strategyDuration(),
				cmd.random());
		gameRepository.saveGame(cmd.toGameId(), game);
		return new UseCaseResult<>(null, game.drainMessages());
	}
}
