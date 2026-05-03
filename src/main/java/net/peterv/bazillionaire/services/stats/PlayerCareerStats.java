package net.peterv.bazillionaire.services.stats;

public record PlayerCareerStats(
    String username,
    long gamesPlayed,
    long wins,
    double winRate,
    long totalEarningsCents,
    long bestGameEarningsCents,
    long totalTradesMade,
    long totalOrdersBlocked,
    long totalPowerupsReceived,
    long totalPowerupsUsed,
    long totalDividendsCollected,
    long totalDividendCashCents,
    long timesFrozen,
    long darkPoolUses) {}
