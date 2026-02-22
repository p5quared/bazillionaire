package net.peterv.bazillionaire.game.port.in;

public interface TickUseCase {
	UseCaseResult<Void> tick(TickCommand cmd);
}
