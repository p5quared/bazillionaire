package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.port.in.TickCommand;
import net.peterv.bazillionaire.game.port.in.TickProgress;
import net.peterv.bazillionaire.game.port.in.TickUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

@ApplicationScoped
public class TickService implements TickUseCase {
  private final GameRepository gameRepository;

  public TickService(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

  @Override
  public UseCaseResult<TickProgress> tick(TickCommand cmd) {
    return gameRepository.withGame(
        cmd.toGameId(),
        game -> {
          game.tick();
          return new UseCaseResult<>(
              new TickProgress(game.currentTick(), game.ticksRemaining()), game.drainMessages());
        });
  }
}
