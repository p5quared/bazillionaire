package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.peterv.bazillionaire.services.stats.PlayerCareerStats;
import net.peterv.bazillionaire.services.stats.PlayerCareerStatsService;
import net.peterv.bazillionaire.services.user.User;

@Path("/leaderboard")
public class LeaderboardController extends Controller {

  @Inject PlayerCareerStatsService careerStatsService;

  @CheckedTemplate
  static class Templates {
    public static native TemplateInstance leaderboard(
        List<LeaderboardEntry> mostWins,
        List<LeaderboardEntry> highestWinRate,
        List<LeaderboardEntry> bestSingleGame,
        List<LeaderboardEntry> mostEarnings);
  }

  public record LeaderboardEntry(String username, String displayValue) {}

  @GET
  @Path("/")
  @Transactional
  public TemplateInstance leaderboard() {
    List<PlayerCareerStats> allStats = getAllCareerStats();

    var mostWins =
        allStats.stream()
            .sorted(Comparator.comparingLong(PlayerCareerStats::wins).reversed())
            .limit(10)
            .map(s -> new LeaderboardEntry(s.username(), s.wins() + " wins"))
            .toList();

    var highestWinRate =
        allStats.stream()
            .filter(s -> s.gamesPlayed() >= 3)
            .sorted(Comparator.comparingDouble(PlayerCareerStats::winRate).reversed())
            .limit(10)
            .map(
                s ->
                    new LeaderboardEntry(
                        s.username(),
                        Math.round(s.winRate() * 100) + "% (" + s.gamesPlayed() + " games)"))
            .toList();

    var bestSingleGame =
        allStats.stream()
            .filter(s -> s.bestGameEarningsCents() > 0)
            .sorted(Comparator.comparingLong(PlayerCareerStats::bestGameEarningsCents).reversed())
            .limit(10)
            .map(s -> new LeaderboardEntry(s.username(), "$" + s.bestGameEarningsCents() / 100))
            .toList();

    var mostEarnings =
        allStats.stream()
            .filter(s -> s.totalEarningsCents() > 0)
            .sorted(Comparator.comparingLong(PlayerCareerStats::totalEarningsCents).reversed())
            .limit(10)
            .map(s -> new LeaderboardEntry(s.username(), "$" + s.totalEarningsCents() / 100))
            .toList();

    return Templates.leaderboard(mostWins, highestWinRate, bestSingleGame, mostEarnings);
  }

  private List<PlayerCareerStats> getAllCareerStats() {
    List<User> users = User.listAll();
    List<PlayerCareerStats> stats = new ArrayList<>();
    for (var user : users) {
      careerStatsService.getCareerStats(user.username).ifPresent(stats::add);
    }
    return stats;
  }
}
