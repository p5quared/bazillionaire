package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.peterv.bazillionaire.services.stats.PlayerGameResult;
import net.peterv.bazillionaire.services.stats.PlayerPortfolioResult;

@Path("/game")
public class RecapController extends Controller {

  @CheckedTemplate
  static class Templates {
    public static native TemplateInstance recap(
        String gameId,
        String winnerName,
        String winnerValueDisplay,
        List<PlayerRecapEntry> players);
  }

  public record PlayerRecapEntry(String username, String valueDisplay, boolean won) {}

  @GET
  @Path("/{id}/recap")
  @Transactional
  public TemplateInstance recap(@PathParam("id") String id) {
    var gameResults = PlayerGameResult.findByGameId(id);
    if (gameResults.isEmpty()) {
      flash("error", "Game not found");
      throw new RedirectException(Response.seeOther(URI.create("/lobby")).build());
    }

    var portfolioResults = PlayerPortfolioResult.findByGameId(id);
    var portfolioMap = new java.util.HashMap<String, Long>();
    for (var pr : portfolioResults) {
      portfolioMap.put(pr.username, pr.finalPortfolioValueCents);
    }

    String winnerName = null;
    String winnerValueDisplay = "";
    List<PlayerRecapEntry> players = new ArrayList<>();

    for (var gr : gameResults) {
      long value = portfolioMap.getOrDefault(gr.username, (long) gr.finalCashCents);
      players.add(new PlayerRecapEntry(gr.username, formatDollars(value), gr.won));
      if (gr.won) {
        winnerName = gr.username;
        winnerValueDisplay = formatDollars(value);
      }
    }

    players.sort(
        (a, b) -> {
          // Sort by won status first, then alphabetically
          if (a.won() != b.won()) return a.won() ? -1 : 1;
          return a.username().compareTo(b.username());
        });

    return Templates.recap(id, winnerName, winnerValueDisplay, players);
  }

  private static String formatDollars(long cents) {
    return String.format("$%,d.%02d", cents / 100, cents % 100);
  }
}
