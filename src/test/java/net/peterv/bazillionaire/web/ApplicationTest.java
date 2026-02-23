package net.peterv.bazillionaire.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
class ApplicationTest {
	private static final String APPLICATION_PATH = "/";

	@Inject
	SessionStore sessionStore;

	@Test
	void redirectsToLoginWhenUserIsNotLoggedIn() {
		given()
				.redirects().follow(false)
				.when()
				.get(APPLICATION_PATH)
				.then()
				.statusCode(303)
				.header("Location", endsWith("/login"));
	}

	@Test
	void showsUsernameWhenUserIsLoggedIn() {
		String username = "test-" + UUID.randomUUID();
		var created = sessionStore.createSession(username);
		var success = assertInstanceOf(SessionStore.CreateSessionResult.Success.class, created);

		given()
				.cookie("SESSION_TOKEN", success.token())
				.when()
				.get(APPLICATION_PATH)
				.then()
				.statusCode(200)
				.body(containsString("welcome, " + username));
	}
}
