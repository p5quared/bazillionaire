package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/")
public class Application extends Controller {

	@Inject
	CurrentSession currentSession;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance index(String username);
	}

	@Path("/")
	public TemplateInstance index() {
		String username = currentSession.getUsername();
		return Templates.index(username);
	}
}
