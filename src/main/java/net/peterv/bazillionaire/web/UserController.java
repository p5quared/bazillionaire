package net.peterv.bazillionaire.web;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import net.peterv.bazillionaire.services.user.User;
import net.peterv.bazillionaire.services.user.UserService;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.util.List;

@Path("/users")
public class UserController extends Controller {

	@Inject
	UserService userService;

	@CheckedTemplate
	static class Templates {
		public static native TemplateInstance list(List<User> users);

		public static native TemplateInstance edit(User user);
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
		return Templates.edit(user);
	}

	@POST
	@Path("/{id}")
	public void update(@PathParam("id") Long id, @RestForm String username) {
		if (username == null || username.isBlank()) {
			flash("error", "Username cannot be blank");
			throw new RedirectException(Response.seeOther(URI.create("/users/" + id)).build());
		}
		userService.updateUser(id, username.strip());
		throw new RedirectException(Response.seeOther(URI.create("/users")).build());
	}

	@POST
	@Path("/{id}/delete")
	public void delete(@PathParam("id") Long id) {
		userService.deleteUser(id);
		throw new RedirectException(Response.seeOther(URI.create("/users")).build());
	}
}
