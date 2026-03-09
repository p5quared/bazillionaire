package net.peterv.bazillionaire.game;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestCreateGameUseCase implements CreateGameUseCase {

	private static volatile boolean failNextCreate;

	@Inject
	GameRepository gameRepository;

	@Override
	public UseCaseResult<Void> createGame(CreateGameCommand cmd) {
		if (failNextCreate) {
			failNextCreate = false;
			throw new IllegalStateException("Simulated game creation failure");
		}

		Game game = Game.create(
				cmd.toPlayerIds(),
				cmd.tickerCount(),
				cmd.initialBalance(),
				cmd.initialPrice(),
				cmd.gameDuration(),
				cmd.random());
		gameRepository.saveGame(cmd.toGameId(), game);
		return new UseCaseResult<>(null, game.drainMessages());
	}

	public static void failNextCreate() {
		failNextCreate = true;
	}

	public static void reset() {
		failNextCreate = false;
	}
}
