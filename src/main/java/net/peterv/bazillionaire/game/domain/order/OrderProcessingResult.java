package net.peterv.bazillionaire.game.domain.order;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameMessage;

public record OrderProcessingResult(OrderResult orderResult, List<GameMessage> messages) {
  public static OrderProcessingResult of(OrderResult result) {
    return new OrderProcessingResult(result, List.of());
  }
}
