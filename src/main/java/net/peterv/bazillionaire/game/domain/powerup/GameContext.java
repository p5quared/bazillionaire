package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;

import java.util.List;
import java.util.Map;

public record GameContext(
        int currentTick,
        Map<PlayerId, GameEvent.PlayerPortfolio> players,
        Map<Symbol, Money> currentPrices,
        List<GameEvent> recentEvents) {
}
