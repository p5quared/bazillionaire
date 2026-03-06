package net.peterv.bazillionaire.game.domain.ticker.regime;

import net.peterv.bazillionaire.game.domain.types.Money;

public interface RegimeStrategy {
	Money priceAt(int tick);
}
