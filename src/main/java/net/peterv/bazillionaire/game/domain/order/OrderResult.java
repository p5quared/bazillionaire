package net.peterv.bazillionaire.game.domain.order;

import net.peterv.bazillionaire.game.domain.types.Money;

public sealed interface OrderResult {
  record Filled(Money fillPrice, Money costBasis) implements OrderResult {}

  record Rejected(String reason) implements OrderResult {}

  record InvalidOrder(String reason) implements OrderResult {}
}
