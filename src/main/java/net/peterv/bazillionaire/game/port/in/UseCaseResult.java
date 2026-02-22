package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.List;

public record UseCaseResult<T>(T result, List<GameMessage> messages) {
	public static <T> UseCaseResult<T> withoutMessages(T result) {
		return new UseCaseResult<>(result, List.of());
	}
}
