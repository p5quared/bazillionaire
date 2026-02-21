package net.peterv.bazillionaire.game.domain.order;

public sealed interface OrderResult {
    record Filled() implements OrderResult {}
    record Rejected(String reason) implements OrderResult {}
    record InvalidOrder(String reason) implements OrderResult {}
}
