package net.peterv.bazillionaire.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.services.auth.SessionStore;
import net.peterv.bazillionaire.services.user.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserControllerTest {

	@Inject
	SessionStore sessionStore;

	private String sessionCookie(String username) {
		var result = (SessionStore.CreateSessionResult.Success) sessionStore.createSession(username);
		return result.token();
	}

	@Test
	void list_requiresAuth() {
		given()
				.redirects().follow(false)
				.when()
				.get("/users")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/login"));
	}

	@Test
	void list_showsUsers() {
		String username = "list-user-" + UUID.randomUUID();
		String token = sessionCookie(username);

		given()
				.cookie("SESSION_TOKEN", token)
				.when()
				.get("/users")
				.then()
				.statusCode(200)
				.body(containsString(username));
	}

	@Test
	void edit_showsForm() {
		String username = "edit-user-" + UUID.randomUUID();
		String token = sessionCookie(username);
		Long userId = User.findByUsername(username).orElseThrow().id;

		given()
				.cookie("SESSION_TOKEN", token)
				.when()
				.get("/users/" + userId)
				.then()
				.statusCode(200)
				.body(containsString(username));
	}

	@Test
	void edit_nonExistentUser_redirects() {
		String token = sessionCookie("edit-missing-" + UUID.randomUUID());

		given()
				.cookie("SESSION_TOKEN", token)
				.redirects().follow(false)
				.when()
				.get("/users/999999")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/users"));
	}

	@Test
	void update_changesUsername() {
		String originalUsername = "update-user-" + UUID.randomUUID();
		String token = sessionCookie(originalUsername);
		Long userId = User.findByUsername(originalUsername).orElseThrow().id;
		String newUsername = "updated-" + UUID.randomUUID();

		given()
				.cookie("SESSION_TOKEN", token)
				.formParam("username", newUsername)
				.redirects().follow(false)
				.when()
				.post("/users/" + userId)
				.then()
				.statusCode(303)
				.header("Location", endsWith("/users"));

		given()
				.cookie("SESSION_TOKEN", token)
				.when()
				.get("/users")
				.then()
				.statusCode(200)
				.body(containsString(newUsername));
	}

	@Test
	void update_blankUsername_flashesError() {
		String username = "blank-user-" + UUID.randomUUID();
		String token = sessionCookie(username);
		Long userId = User.findByUsername(username).orElseThrow().id;

		given()
				.cookie("SESSION_TOKEN", token)
				.formParam("username", "")
				.redirects().follow(false)
				.when()
				.post("/users/" + userId)
				.then()
				.statusCode(303)
				.header("Location", endsWith("/users/" + userId));
	}

	@Test
	void delete_removesUser() {
		String username = "delete-user-" + UUID.randomUUID();
		String token = sessionCookie(username);
		Long userId = User.findByUsername(username).orElseThrow().id;

		given()
				.cookie("SESSION_TOKEN", token)
				.redirects().follow(false)
				.when()
				.post("/users/" + userId + "/delete")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/users"));

		given()
				.cookie("SESSION_TOKEN", token)
				.when()
				.get("/users")
				.then()
				.statusCode(200)
				.body(not(containsString(username)));
	}
}
