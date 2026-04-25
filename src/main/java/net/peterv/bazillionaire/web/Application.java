package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import java.util.List;
import net.peterv.bazillionaire.services.lobby.Lobby;
import net.peterv.bazillionaire.services.stats.PlayerCareerStats;
import net.peterv.bazillionaire.services.stats.PlayerCareerStatsService;

@Path("/")
public class Application extends Controller {

  @Inject CurrentSession currentSession;
  @Inject PlayerCareerStatsService careerStatsService;

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(
        String username, HomeStats stats, List<Lobby> lobbies, int lobbyCount, int traderCount);
  }

  public record HomeStats(
      long gamesPlayed,
      long wins,
      long losses,
      int winPct,
      String bestGame,
      String record,
      String totalEarnings) {

    public static HomeStats from(PlayerCareerStats s) {
      long losses = s.gamesPlayed() - s.wins();
      int winPct = s.gamesPlayed() > 0 ? (int) Math.round(s.winRate() * 100) : 0;
      return new HomeStats(
          s.gamesPlayed(),
          s.wins(),
          losses,
          winPct,
          formatDollars(s.bestGameValueCents()),
          s.wins() + "W\u2013" + losses + "L",
          formatDollars(s.totalEarningsCents()));
    }

    private static String formatDollars(long cents) {
      if (cents == 0) return "$0";
      long dollars = cents / 100;
      if (dollars >= 1_000_000) {
        return "$" + String.format("%.1fM", dollars / 1_000_000.0);
      } else if (dollars >= 1_000) {
        return "$" + String.format("%,d", dollars);
      }
      return "$" + String.format("%.2f", cents / 100.0);
    }
  }

  @Path("/")
  @Transactional
  public TemplateInstance index() {
    String username = currentSession.getUsername();
    HomeStats stats = careerStatsService.getCareerStats(username).map(HomeStats::from).orElse(null);
    List<Lobby> lobbies = Lobby.findOpen();
    int traderCount = lobbies.stream().mapToInt(l -> l.members.size()).sum();
    return Templates.index(username, stats, lobbies, lobbies.size(), traderCount);
  }
}
