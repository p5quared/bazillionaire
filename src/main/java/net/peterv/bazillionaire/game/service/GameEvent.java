package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public sealed interface GameEvent permits GameEvent.OrderFilled {
    record OrderFilled(Order order, PlayerId playerId) implements GameEvent {}
}
