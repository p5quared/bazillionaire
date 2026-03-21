package net.peterv.bazillionaire.game.port.out;

import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public record GameFinishedSnapshot(
    Map<PlayerId, GameEvent.PlayerPortfolio> players, Map<Symbol, Money> finalPrices) {}
