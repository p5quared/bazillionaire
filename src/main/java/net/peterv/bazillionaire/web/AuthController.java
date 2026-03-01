package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.jboss.resteasy.reactive.RestForm;

@Path("/")
public class AuthController extends Controller {

	@Inject
	SessionStore sessionStore;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance login();
	}

	public TemplateInstance login() {
		return Templates.login();
	}

	@POST
	public void login(@RestForm String username) {
		if (username == null || username.isBlank()) {
			validation.addError("username", "Username is required");
			validationFailed();
		}

		username = username.strip();

		var result = sessionStore.createSession(username);
		switch (result) {
			case SessionStore.CreateSessionResult.Success success -> {
				NewCookie cookie = new NewCookie.Builder("SESSION_TOKEN")
						.value(success.token())
						.path("/")
						.httpOnly(true)
						.sameSite(NewCookie.SameSite.STRICT)
						.build();
				throw new io.quarkiverse.renarde.util.RedirectException(
						Response.seeOther(URI.create("/"))
								.cookie(cookie)
								.build());
			}
			case SessionStore.CreateSessionResult.UsernameTaken() -> {
				flash("error", "Username '" + username + "' is already taken");
				login();
			}
		}
	}
}
