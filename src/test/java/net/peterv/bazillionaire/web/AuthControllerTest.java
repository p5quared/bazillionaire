package net.peterv.bazillionaire.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class AuthControllerTest {

	private static final String LOGIN_PATH = "/login";
	private static final String INDEX_PAGE_PATH = "/";

	@Inject
	SessionStore sessionStore;

	@Test
	void showsLoginPage() {
		given()
				.when()
				.get(LOGIN_PATH)
				.then()
				.statusCode(200)
				.body(containsString("Choose a username:"));
	}

	@Test
	void logsInAndSetsSessionCookie() {
		String randomUsername = "login-test-" + UUID.randomUUID();

		var response = given()
				.redirects().follow(false)
				.formParam("username", randomUsername)
				.when()
				.post(LOGIN_PATH)
				.then()
				.statusCode(303)
				.header("Location", endsWith(INDEX_PAGE_PATH))
				.extract()
				.response();

		String sessionToken = response.getCookie("SESSION_TOKEN");
		assertNotNull(sessionToken);
		assertEquals(randomUsername, sessionStore.getUsername(sessionToken).orElseThrow());
	}
}
