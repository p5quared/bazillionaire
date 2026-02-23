package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

import java.util.Random;

public class CreateGameService implements CreateGameUseCase {
	private final static int DEFAULT_BALANCE = 100_000_00;
	private final static int DEFAULT_TICKER_PRICE = 100_00;
	private final static int DEFAULT_GAME_DURATION = 200; // ticks
	private final static int DEFAULT_STRATEGY_DURATION = 25; // ticks

	private final GameRepository gameRepository;

	public CreateGameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<Void> createGame(CreateGameCommand cmd) {
		Game game = Game.create(
				cmd.toPlayerIds(),
				cmd.tickerCount(),
				new Money(DEFAULT_BALANCE),
				new Money(DEFAULT_TICKER_PRICE),
				DEFAULT_GAME_DURATION,
				DEFAULT_STRATEGY_DURATION,
				new Random());
		gameRepository.saveGame(cmd.toGameId(), game);
		return new UseCaseResult<>(null, game.drainMessages());
	}
}
