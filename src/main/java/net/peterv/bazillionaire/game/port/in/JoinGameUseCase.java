package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.JoinResult;

public interface JoinGameUseCase {
	UseCaseResult<JoinResult> join(JoinGameCommand cmd);
}
