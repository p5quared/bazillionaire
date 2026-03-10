package net.peterv.bazillionaire.game.domain.ticker.regime;

import java.util.List;
import net.peterv.bazillionaire.game.domain.types.Money;

public interface RegimeStrategy {
  List<Money> prices();
}
