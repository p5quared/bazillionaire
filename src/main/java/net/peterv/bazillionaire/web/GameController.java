package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/game")
public class GameController extends Controller {

	@Inject
	CurrentSession currentSession;

	@CheckedTemplate
	static class Templates {
		public static native TemplateInstance game(String gameId, String playerId);
	}

	@GET
	@Path("/{id}")
	public TemplateInstance game(@PathParam("id") String id) {
		return Templates.game(id, currentSession.getUsername());
	}
}
