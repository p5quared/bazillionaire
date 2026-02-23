package net.peterv.bazillionaire.game.port.in;

public interface CreateGameUseCase {
	UseCaseResult<Void> createGame(CreateGameCommand cmd);
}
