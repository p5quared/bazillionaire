package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

import java.util.List;

public interface RegimeStrategy {
	List<Money> prices();
}
