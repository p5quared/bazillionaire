package net.peterv.bazillionaire.game.domain.order;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public sealed interface Order {
	Symbol symbol();

	Money price();

	record Buy(Symbol symbol, Money price) implements Order {
	}

	record Sell(Symbol symbol, Money price) implements Order {
	}
}
