package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.port.in.StartGameCommand;
import net.peterv.bazillionaire.game.port.in.StartGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

@ApplicationScoped
public class StartGameService implements StartGameUseCase {
    private final GameRepository gameRepository;

    public StartGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public UseCaseResult<Void> startGame(StartGameCommand cmd) {
        return gameRepository.withGame(cmd.toGameId(), game -> {
            game.start();
            return new UseCaseResult<>(null, game.drainMessages());
        });
    }
}
