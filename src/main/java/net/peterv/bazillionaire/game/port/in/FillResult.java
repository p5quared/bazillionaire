package net.peterv.bazillionaire.game.port.in;

public sealed interface FillResult {
    record Filled() implements FillResult {}
    record Rejected(String reason) implements FillResult {}
}
