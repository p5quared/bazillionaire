package net.peterv.bazillionaire.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

@Provider
public class SessionFilter implements ContainerRequestFilter {

	@Inject
	SessionStore sessionStore;

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
			abort(requestContext);
			return;
		}

		var username = sessionStore.getUsername(cookie.getValue());
		if (username.isEmpty()) {
			abort(requestContext);
			return;
		}

		currentSession.setUsername(username.get());
	}

	private void abort(ContainerRequestContext requestContext) {
		requestContext.abortWith(
				Response.seeOther(URI.create("/login")).build());
	}
}
