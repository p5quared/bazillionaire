package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.JoinResult;
import net.peterv.bazillionaire.game.port.in.JoinGameCommand;
import net.peterv.bazillionaire.game.port.in.JoinGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

import java.util.List;

@ApplicationScoped
public class JoinGameService implements JoinGameUseCase {
	private final GameRepository gameRepository;

	public JoinGameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<JoinResult> join(JoinGameCommand cmd) {
		return gameRepository.withGame(cmd.toGameId(), game -> {
			JoinResult result = game.join(cmd.toPlayerId());
			List<GameMessage> messages = game.drainMessages();
			return new UseCaseResult<>(result, messages);
		});
	}
}
