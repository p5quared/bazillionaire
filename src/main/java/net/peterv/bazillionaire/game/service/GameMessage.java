package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public record GameMessage(GameEvent event, Audience audience) {
	public static GameMessage broadcast(GameEvent gameEvent) {
		return new GameMessage(gameEvent, new Audience.Everyone());
	}

	public static GameMessage send(GameEvent gameEvent, PlayerId playerId) {
		return new GameMessage(gameEvent, new Audience.Only(playerId));
	}
}
