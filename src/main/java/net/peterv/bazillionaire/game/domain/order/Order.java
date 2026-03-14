package net.peterv.bazillionaire.game.domain.order;

import net.peterv.bazillionaire.game.domain.types.Symbol;

public sealed interface Order {
  Symbol symbol();

  record Buy(Symbol symbol) implements Order {}

  record Sell(Symbol symbol) implements Order {}
}
