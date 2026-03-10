package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public record GameContext(
    int currentTick,
    Map<PlayerId, GameEvent.PlayerPortfolio> players,
    Map<Symbol, Money> currentPrices,
    List<GameEvent> recentEvents) {}
