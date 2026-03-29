package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import net.peterv.bazillionaire.services.lobby.Lobby;
import net.peterv.bazillionaire.services.lobby.LobbyMemberRequiredException;
import net.peterv.bazillionaire.services.lobby.LobbyNotFoundException;
import net.peterv.bazillionaire.services.lobby.LobbyService;
import net.peterv.bazillionaire.services.lobby.LobbyStartFailedException;
import org.jboss.resteasy.reactive.RestForm;

@Path("/lobby")
public class LobbyController extends Controller {

  @Inject CurrentSession currentSession;

  @Inject LobbyService lobbyService;

  @CheckedTemplate
  static class Templates {
    public static native TemplateInstance list(List<Lobby> lobbies);

    public static native TemplateInstance detail(
        Lobby lobby, boolean currentPlayerIsMember, int minPlayers);
  }

  @GET
  @Path("/")
  @Transactional
  public TemplateInstance list() {
    var lobbies = Lobby.findOpen();
    return Templates.list(lobbies);
  }

  @POST
  @Path("/")
  public void create(@RestForm String name, @RestForm Integer maxPlayers) {
    if (name == null || name.isBlank()) {
      flash("error", "Lobby name is required");
      throw redirect("/lobby");
    }
    int max =
        maxPlayers != null
            ? Math.min(Math.max(maxPlayers, Lobby.MIN_PLAYERS), Lobby.MAX_HARD_CAP)
            : Lobby.DEFAULT_MAX;
    String id = lobbyService.createLobby(name.strip(), max, currentSession.getUsername());
    throw redirect("/lobby/" + id);
  }

  @GET
  @Path("/{id}")
  @Transactional
  public TemplateInstance detail(@PathParam("id") String id) {
    Lobby lobby = Lobby.findById(id);
    if (lobby == null) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    }
    if (lobby.status == Lobby.LobbyStatus.STARTED) {
      throw redirect("/game/" + id);
    }
    return Templates.detail(
        lobby, lobby.hasMember(currentSession.getUsername()), Lobby.MIN_PLAYERS);
  }

  @POST
  @Path("/{id}/join")
  public void join(@PathParam("id") String id) {
    try {
      lobbyService.joinLobby(id, currentSession.getUsername());
    } catch (LobbyNotFoundException e) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    } catch (Lobby.LobbyFullException e) {
      flash("error", "Lobby is full");
      throw redirect("/lobby/" + id);
    } catch (Lobby.LobbyNotOpenException e) {
      flash("error", "Lobby is not open");
      throw redirect("/lobby/" + id);
    }
    throw redirect("/lobby/" + id);
  }

  @POST
  @Path("/{id}/leave")
  public void leave(@PathParam("id") String id) {
    try {
      lobbyService.leaveLobby(id, currentSession.getUsername());
    } catch (LobbyNotFoundException e) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    } catch (Lobby.LobbyNotOpenException e) {
      flash("error", "Lobby is not open");
      throw redirect("/lobby/" + id);
    }
    throw redirect("/lobby");
  }

  @POST
  @Path("/{id}/settings")
  public void settings(
      @PathParam("id") String id,
      @RestForm int tickerCount,
      @RestForm int initialBalance,
      @RestForm int gameDuration) {
    try {
      lobbyService.updateSettings(
          id, currentSession.getUsername(), tickerCount, initialBalance * 100, gameDuration);
    } catch (LobbyNotFoundException e) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    } catch (LobbyMemberRequiredException e) {
      flash("error", "Only lobby members can change settings");
      throw redirect("/lobby/" + id);
    } catch (Lobby.LobbyNotOpenException e) {
      flash("error", "Lobby is not open");
      throw redirect("/lobby/" + id);
    }
    throw redirect("/lobby/" + id);
  }

  @POST
  @Path("/{id}/start")
  public void start(@PathParam("id") String id) {
    try {
      lobbyService.startLobby(id, currentSession.getUsername());
    } catch (Lobby.NotEnoughPlayersException e) {
      flash("error", "Need at least " + Lobby.MIN_PLAYERS + " players to start");
      throw redirect("/lobby/" + id);
    } catch (Lobby.LobbyNotOpenException e) {
      flash("error", "Lobby is not open");
      throw redirect("/lobby/" + id);
    } catch (LobbyMemberRequiredException e) {
      flash("error", "Only lobby members can start the game");
      throw redirect("/lobby/" + id);
    } catch (LobbyNotFoundException e) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    } catch (LobbyStartFailedException e) {
      flash("error", "Unable to start the game right now");
      throw redirect("/lobby/" + id);
    }
    throw redirect("/game/" + id);
  }

  @POST
  @Path("/{id}/delete")
  public void delete(@PathParam("id") String id) {
    try {
      lobbyService.deleteLobby(id, currentSession.getUsername());
    } catch (Lobby.LobbyNotOpenException e) {
      flash("error", "Cannot delete a lobby that has already started");
      throw redirect("/lobby/" + id);
    } catch (LobbyMemberRequiredException e) {
      flash("error", "Only lobby members can delete this lobby");
      throw redirect("/lobby/" + id);
    } catch (LobbyNotFoundException e) {
      flash("error", "Lobby not found");
      throw redirect("/lobby");
    }
    throw redirect("/lobby");
  }

  @GET
  @Path("/{id}/status")
  @Produces(MediaType.APPLICATION_JSON)
  @Transactional
  public LobbyStatusResponse status(@PathParam("id") String id) {
    Lobby lobby = Lobby.findById(id);
    if (lobby == null) {
      return new LobbyStatusResponse(id, "NOT_FOUND", List.of(), null);
    }
    var memberNames = lobby.members.stream().map(m -> m.displayName).toList();
    String redirectUrl = lobby.status == Lobby.LobbyStatus.STARTED ? "/game/" + id : null;
    return new LobbyStatusResponse(id, lobby.status.name(), memberNames, redirectUrl);
  }

  public record LobbyStatusResponse(
      String id, String status, List<String> members, String redirectUrl) {}

  private RedirectException redirect(String path) {
    return new RedirectException(Response.seeOther(URI.create(path)).build());
  }
}
