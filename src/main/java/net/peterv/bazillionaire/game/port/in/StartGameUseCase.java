package net.peterv.bazillionaire.game.port.in;

public interface StartGameUseCase {
    UseCaseResult<Void> startGame(StartGameCommand cmd);
}
