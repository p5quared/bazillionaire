package net.peterv.bazillionaire.game.port.in;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameMessage;

public record UseCaseResult<T>(T result, List<GameMessage> messages) {
  public static <T> UseCaseResult<T> withoutMessages(T result) {
    return new UseCaseResult<>(result, List.of());
  }
}
