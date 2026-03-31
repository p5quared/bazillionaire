package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import net.peterv.bazillionaire.services.stats.PlayerCareerStats;
import net.peterv.bazillionaire.services.stats.PlayerCareerStatsService;
import net.peterv.bazillionaire.services.user.User;
import net.peterv.bazillionaire.services.user.UserService;
import org.jboss.resteasy.reactive.RestForm;

@Path("/users")
public class UserController extends Controller {

  @Inject UserService userService;
  @Inject PlayerCareerStatsService careerStatsService;

  @CheckedTemplate
  static class Templates {
    public static native TemplateInstance list(List<User> users);

    public static native TemplateInstance edit(User user, CareerStatsDisplay careerStats);
  }

  public record CareerStatsDisplay(
      String record,
      String totalEarnings,
      String bestGame,
      long totalTradesMade,
      long totalOrdersBlocked,
      long totalPowerupsReceived,
      long totalPowerupsUsed,
      String dividends,
      long timesFrozen,
      long darkPoolUses) {

    static CareerStatsDisplay from(PlayerCareerStats stats) {
      long losses = stats.gamesPlayed() - stats.wins();
      int winPct = (int) Math.round(stats.winRate() * 100);
      return new CareerStatsDisplay(
          stats.wins() + "W - " + losses + "L (" + winPct + "%)",
          formatDollars(stats.totalEarningsCents()),
          formatDollars(stats.bestGameValueCents()),
          stats.totalTradesMade(),
          stats.totalOrdersBlocked(),
          stats.totalPowerupsReceived(),
          stats.totalPowerupsUsed(),
          stats.totalDividendsCollected()
              + " ("
              + formatDollars(stats.totalDividendCashCents())
              + ")",
          stats.timesFrozen(),
          stats.darkPoolUses());
    }

    private static String formatDollars(long cents) {
      return String.format("$%,d.%02d", cents / 100, cents % 100);
    }
  }

  @GET
  @Path("/")
  @Transactional
  public TemplateInstance list() {
    List<User> users = User.listAll();
    return Templates.list(users);
  }

  @GET
  @Path("/{id}")
  @Transactional
  public TemplateInstance edit(@PathParam("id") Long id) {
    User user = User.findById(id);
    if (user == null) {
      flash("error", "User not found");
      throw new RedirectException(Response.seeOther(URI.create("/users")).build());
    }
    var careerStats =
        careerStatsService.getCareerStats(user.username).map(CareerStatsDisplay::from).orElse(null);
    return Templates.edit(user, careerStats);
  }

  @POST
  @Path("/{id}")
  public void update(@PathParam("id") Long id, @RestForm String username) {
    if (username == null || username.isBlank()) {
      flash("error", "Username cannot be blank");
      throw new RedirectException(Response.seeOther(URI.create("/users/" + id)).build());
    }
    userService.updateUsername(id, username.strip());
    throw new RedirectException(Response.seeOther(URI.create("/users")).build());
  }

  @POST
  @Path("/{id}/delete")
  public void delete(@PathParam("id") Long id) {
    userService.deleteUser(id);
    throw new RedirectException(Response.seeOther(URI.create("/users")).build());
  }
}
