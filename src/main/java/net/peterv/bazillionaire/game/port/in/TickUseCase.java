package net.peterv.bazillionaire.game.port.in;

public interface TickUseCase {
  UseCaseResult<TickProgress> tick(TickCommand cmd);
}
