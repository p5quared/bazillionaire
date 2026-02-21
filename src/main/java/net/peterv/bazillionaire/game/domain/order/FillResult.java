package net.peterv.bazillionaire.game.domain.order;

public sealed interface FillResult {
    record Filled() implements FillResult {}
    record Rejected(String reason) implements FillResult {}
}
