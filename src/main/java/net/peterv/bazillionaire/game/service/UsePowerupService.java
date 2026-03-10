package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;
import net.peterv.bazillionaire.game.port.in.UsePowerupCommand;
import net.peterv.bazillionaire.game.port.in.UsePowerupUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

import net.peterv.bazillionaire.game.domain.event.GameMessage;

import java.util.List;

@ApplicationScoped
public class UsePowerupService implements UsePowerupUseCase {
	private final GameRepository gameRepository;

	public UsePowerupService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<UsePowerupResult> usePowerup(UsePowerupCommand cmd) {
		return gameRepository.withGame(cmd.toGameId(), game -> {
			UsePowerupResult result = game.usePowerup(cmd.toPlayerId(), cmd.powerupName(), cmd.quantity(),
					cmd.toTargetPlayerId());
			List<GameMessage> messages = game.drainMessages();
			return new UseCaseResult<>(result, messages);
		});
	}
}
