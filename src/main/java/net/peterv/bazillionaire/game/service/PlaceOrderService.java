package net.peterv.bazillionaire.game.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand;
import net.peterv.bazillionaire.game.port.in.PlaceOrderUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameRepository;

import java.util.List;

@ApplicationScoped
public class PlaceOrderService implements PlaceOrderUseCase {
	private final GameRepository gameRepository;

	public PlaceOrderService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Override
	public UseCaseResult<OrderResult> placeOrder(PlaceOrderCommand cmd) {
		Order order = cmd.toOrder();
		return gameRepository.withGame(cmd.toGameId(), game -> {
			OrderResult orderResult = game.placeOrder(order, cmd.toPlayerId());
			List<GameMessage> gameMessages = game.drainMessages();
			return new UseCaseResult<>(orderResult, gameMessages);
		});
	}
}
