package net.peterv.bazillionaire.web;

import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import net.peterv.bazillionaire.services.auth.AuthService;

import java.net.URI;

@Blocking
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SessionFilter implements ContainerRequestFilter {

	@Inject
	AuthService authService;

	@Inject
	CurrentSession currentSession;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		String path = requestContext.getUriInfo().getPath();

		if (path.startsWith("/login") || path.startsWith("/_renarde/") || path.startsWith("/q/")) {
			return;
		}

		Cookie cookie = requestContext.getCookies().get("SESSION_TOKEN");
		if (cookie == null) {
			redirectLogin(requestContext);
			return;
		}

		var username = authService.getUsername(cookie.getValue());
		if (username.isEmpty()) {
			redirectLogin(requestContext);
			return;
		}

		currentSession.setUsername(username.get());
	}

	private void redirectLogin(ContainerRequestContext requestContext) {
		requestContext.abortWith(
				Response.seeOther(URI.create("/login")).build());
	}
}
