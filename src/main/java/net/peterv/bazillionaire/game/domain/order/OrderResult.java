package net.peterv.bazillionaire.game.domain.order;

import net.peterv.bazillionaire.game.service.GameMessage;

public sealed interface OrderResult {
    record Filled(GameMessage message) implements OrderResult {}
    record Rejected(String reason) implements OrderResult {}
    record InvalidOrder(String reason) implements OrderResult {}
}
