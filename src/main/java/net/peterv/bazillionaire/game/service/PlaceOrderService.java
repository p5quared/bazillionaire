package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand;
import net.peterv.bazillionaire.game.port.in.PlaceOrderUseCase;
import net.peterv.bazillionaire.game.port.out.GameRepository;

public class PlaceOrderService implements PlaceOrderUseCase {
    private final GameRepository gameRepository;

    public PlaceOrderService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public OrderResult placeOrder(PlaceOrderCommand cmd) {
        Game game = this.gameRepository.loadGame(cmd.toGameId());
        Order order = cmd.toOrder();
        OrderResult result = game.placeOrder(order, cmd.toPlayerId());
        this.gameRepository.saveGame(cmd.toGameId(), game);
        return result;
    }
}
