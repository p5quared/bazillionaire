package net.peterv.bazillionaire.services.lobby;

import net.peterv.bazillionaire.game.domain.types.Money;

public record StartLobbySettings(
    int tickerCount, Money initialBalance, Money initialPrice, int gameDuration) {}
