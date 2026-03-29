package net.peterv.bazillionaire.game.domain.powerup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class DividendTrigger implements PowerupTrigger {
  private final int payoutInterval;
  private final Map<Symbol, Money> initialPrices;
  private final Map<PlayerId, Map<Symbol, Integer>> holdingSince = new HashMap<>();

  public DividendTrigger(int payoutInterval, Map<Symbol, Money> initialPrices) {
    this.payoutInterval = payoutInterval;
    this.initialPrices = Map.copyOf(initialPrices);
  }

  @Override
  public List<AwardedPowerup> evaluate(GameContext context) {
    updateHoldTracking(context);

    if (context.currentTick() == 0 || context.currentTick() % payoutInterval != 0) {
      return List.of();
    }

    List<AwardedPowerup> awards = new ArrayList<>();
    for (var playerEntry : holdingSince.entrySet()) {
      PlayerId playerId = playerEntry.getKey();
      GameEvent.PlayerPortfolio portfolio = context.players().get(playerId);
      if (portfolio == null) {
        continue;
      }
      for (var symbolEntry : playerEntry.getValue().entrySet()) {
        Symbol symbol = symbolEntry.getKey();
        if (context.delistedSymbols().contains(symbol)) continue;
        int since = symbolEntry.getValue();
        int shares = portfolio.holdings().getOrDefault(symbol, 0);
        int holdDuration = context.currentTick() - since;
        DividendTier tier = DividendTier.highestQualifying(shares, holdDuration);
        if (tier != null) {
          Money symbolInitialPrice = initialPrices.get(symbol);
          if (symbolInitialPrice == null) continue;
          Money currentPrice = context.currentPrices().getOrDefault(symbol, symbolInitialPrice);
          long blendedPriceCents = (symbolInitialPrice.cents() + currentPrice.cents()) / 2;
          long payout = (long) tier.yieldBasisPoints() * blendedPriceCents * shares / 10000;
          Money payoutMoney = new Money((int) payout);
          awards.add(
              new AwardedPowerup(
                  playerId,
                  new DividendPowerup(playerId, payoutMoney, symbol, tier.displayName())));
        }
      }
    }
    return awards;
  }

  private void updateHoldTracking(GameContext context) {
    for (var playerEntry : context.players().entrySet()) {
      PlayerId playerId = playerEntry.getKey();
      GameEvent.PlayerPortfolio portfolio = playerEntry.getValue();
      Map<Symbol, Integer> playerHoldings =
          holdingSince.computeIfAbsent(playerId, k -> new HashMap<>());

      for (var holdingEntry : portfolio.holdings().entrySet()) {
        Symbol symbol = holdingEntry.getKey();
        int shares = holdingEntry.getValue();
        if (shares >= 3) {
          playerHoldings.putIfAbsent(symbol, context.currentTick());
        } else {
          playerHoldings.remove(symbol);
        }
      }

      // Remove symbols the player no longer holds at all
      playerHoldings.keySet().removeIf(symbol -> !portfolio.holdings().containsKey(symbol));
    }
  }
}
