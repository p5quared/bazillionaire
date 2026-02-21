package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.order.OrderResult;

public interface PlaceOrderUseCase {
    UseCaseResult<OrderResult> placeOrder(PlaceOrderCommand cmd);
}
