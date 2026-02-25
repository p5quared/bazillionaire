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
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.util.List;

@Path("/lobby")
public class LobbyController extends Controller {

	@Inject
	CurrentSession currentSession;

	@Inject
	LobbyService lobbyService;

	@Inject
	CreateGameUseCase createGameUseCase;

	@CheckedTemplate
	static class Templates {
		public static native TemplateInstance list(List<Lobby> lobbies);

		public static native TemplateInstance detail(Lobby lobby, String currentPlayerId, int minPlayers);
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
			throw new RedirectException(Response.seeOther(URI.create("/lobby")).build());
		}
		int max = maxPlayers != null
				? Math.min(Math.max(maxPlayers, Lobby.MIN_PLAYERS), Lobby.MAX_HARD_CAP)
				: Lobby.DEFAULT_MAX;
		String id = lobbyService.createLobby(name.strip(), max, currentSession.getUsername());
		throw new RedirectException(Response.seeOther(URI.create("/lobby/" + id)).build());
	}

	@GET
	@Path("/{id}")
	@Transactional
	public TemplateInstance detail(@PathParam("id") String id) {
		Lobby lobby = Lobby.findById(id);
		if (lobby == null) {
			flash("error", "Lobby not found");
			throw new RedirectException(Response.seeOther(URI.create("/lobby")).build());
		}
		if (lobby.status == Lobby.LobbyStatus.STARTED) {
			throw new RedirectException(Response.seeOther(URI.create("/game/" + id)).build());
		}
		return Templates.detail(lobby, currentSession.getUsername(), Lobby.MIN_PLAYERS);
	}

	@POST
	@Path("/{id}/join")
	public void join(@PathParam("id") String id) {
		lobbyService.joinLobby(id, currentSession.getUsername());
		throw new RedirectException(Response.seeOther(URI.create("/lobby/" + id)).build());
	}

	@POST
	@Path("/{id}/leave")
	public void leave(@PathParam("id") String id) {
		lobbyService.leaveLobby(id, currentSession.getUsername());
		throw new RedirectException(Response.seeOther(URI.create("/lobby")).build());
	}

	@POST
	@Path("/{id}/start")
	public void start(@PathParam("id") String id) {
		try {
			var cmd = lobbyService.startLobby(id);
			createGameUseCase.createGame(cmd);
		} catch (Lobby.NotEnoughPlayersException e) {
			flash("error", "Need at least " + Lobby.MIN_PLAYERS + " players to start");
			throw new RedirectException(Response.seeOther(URI.create("/lobby/" + id)).build());
		} catch (Lobby.LobbyNotOpenException e) {
			flash("error", "Lobby is not open");
			throw new RedirectException(Response.seeOther(URI.create("/lobby/" + id)).build());
		}
		throw new RedirectException(Response.seeOther(URI.create("/game/" + id)).build());
	}

	@POST
	@Path("/{id}/delete")
	public void delete(@PathParam("id") String id) {
		try {
			lobbyService.deleteLobby(id);
		} catch (Lobby.LobbyNotOpenException e) {
			flash("error", "Cannot delete a lobby that has already started");
			throw new RedirectException(Response.seeOther(URI.create("/lobby/" + id)).build());
		}
		throw new RedirectException(Response.seeOther(URI.create("/lobby")).build());
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

	public record LobbyStatusResponse(String id, String status, List<String> members, String redirectUrl) {
	}
}
