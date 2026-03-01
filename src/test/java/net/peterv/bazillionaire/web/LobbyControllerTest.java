package net.peterv.bazillionaire.web;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.services.auth.AuthService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LobbyControllerTest {

	@Inject
	AuthService authService;

	private String sessionCookie(String username) {
		var result = (AuthService.CreateSessionResult.Success) authService.createSession(username);
		return result.token();
	}

	/**
	 * Creates a lobby and returns its 6-char ID extracted from the Location header.
	 */
	private String createLobby(String token, String name) {
		var response = given()
				.cookie("SESSION_TOKEN", token)
				.formParam("name", name)
				.formParam("maxPlayers", "8")
				.redirects().follow(false)
				.when()
				.post("/lobby")
				.then()
				.statusCode(303)
				.extract()
				.response();
		String location = response.getHeader("Location");
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private void joinLobby(String token, String lobbyId) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/join");
	}

	private void startLobby(String token, String lobbyId) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/start");
	}

	private void checkJoin(String token, String lobbyId, String locationSuffix) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/join")
				.then().statusCode(303).header("Location", endsWith(locationSuffix));
	}

	private void checkLeave(String token, String lobbyId, String locationSuffix) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/leave")
				.then().statusCode(303).header("Location", endsWith(locationSuffix));
	}

	private void checkStart(String token, String lobbyId, String locationSuffix) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/start")
				.then().statusCode(303).header("Location", endsWith(locationSuffix));
	}

	private void checkDelete(String token, String lobbyId, String locationSuffix) {
		given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().post("/lobby/" + lobbyId + "/delete")
				.then().statusCode(303).header("Location", endsWith(locationSuffix));
	}

	private ValidatableResponse checkGetLobby(String token, String lobbyId) {
		return given().cookie("SESSION_TOKEN", token).redirects().follow(false)
				.when().get("/lobby/" + lobbyId)
				.then();
	}

	private ValidatableResponse checkGetStatus(String token, String lobbyId) {
		return given().cookie("SESSION_TOKEN", token)
				.when().get("/lobby/" + lobbyId + "/status")
				.then().statusCode(200).contentType(containsString("application/json"));
	}

	@Test
	void list_requiresAuth() {
		given()
				.redirects().follow(false)
				.when()
				.get("/lobby")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/login"));
	}

	@Test
	void list_showsLobbiesPage() {
		String token = sessionCookie("list-user-" + UUID.randomUUID());
		given()
				.cookie("SESSION_TOKEN", token)
				.when()
				.get("/lobby")
				.then()
				.statusCode(200)
				.body(containsString("Create"));
	}

	@Test
	void create_createsLobbyAndRedirects() {
		String token = sessionCookie("creator-" + UUID.randomUUID());
		given()
				.cookie("SESSION_TOKEN", token)
				.formParam("name", "Test Lobby")
				.formParam("maxPlayers", "4")
				.redirects().follow(false)
				.when()
				.post("/lobby")
				.then()
				.statusCode(303)
				.header("Location", matchesRegex(".*/lobby/[A-Z0-9]{6}$"));
	}

	@Test
	void create_requiresName() {
		String token = sessionCookie("creator2-" + UUID.randomUUID());
		given()
				.cookie("SESSION_TOKEN", token)
				.formParam("name", "")
				.redirects().follow(false)
				.when()
				.post("/lobby")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/lobby"));
	}

	@Test
	void detail_showsWaitingRoom() {
		String token = sessionCookie("detail-user-" + UUID.randomUUID());
		String lobbyId = createLobby(token, "Detail Test Lobby");
		checkGetLobby(token, lobbyId).statusCode(200).body(containsString("Detail Test Lobby"));
	}

	@Test
	void detail_redirectsIfStarted() {
		String aliceToken = sessionCookie("alice-" + UUID.randomUUID());
		String bobToken = sessionCookie("bob-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Started Lobby");

		joinLobby(bobToken, lobbyId);
		checkStart(aliceToken, lobbyId, "/game/" + lobbyId);
		checkGetLobby(aliceToken, lobbyId).statusCode(303).header("Location", endsWith("/game/" + lobbyId));
	}

	@Test
	void join_addsPlayer() {
		String aliceToken = sessionCookie("alice2-" + UUID.randomUUID());
		String bobUsername = "bob2-" + UUID.randomUUID();
		String bobToken = sessionCookie(bobUsername);
		String lobbyId = createLobby(aliceToken, "Join Test Lobby");

		checkJoin(bobToken, lobbyId, "/lobby/" + lobbyId);
		checkGetLobby(aliceToken, lobbyId).statusCode(200).body(containsString(bobUsername));
	}

	@Test
	void join_idempotent() {
		String aliceToken = sessionCookie("alice3-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Idempotent Lobby");

		checkJoin(aliceToken, lobbyId, "/lobby/" + lobbyId);
	}

	@Test
	void leave_removesPlayer() {
		String aliceToken = sessionCookie("alice4-" + UUID.randomUUID());
		String bobUsername = "bob4-" + UUID.randomUUID();
		String bobToken = sessionCookie(bobUsername);
		String lobbyId = createLobby(aliceToken, "Leave Test Lobby");

		joinLobby(bobToken, lobbyId);
		checkLeave(bobToken, lobbyId, "/lobby");
		checkGetLobby(aliceToken, lobbyId).statusCode(200).body(not(containsString(bobUsername)));
	}

	@Test
	void start_requiresMinPlayers() {
		String aliceToken = sessionCookie("alice5-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Min Players Lobby");

		checkStart(aliceToken, lobbyId, "/game/" + lobbyId);
	}

	@Test
	void start_startsGameAndRedirects() {
		String aliceToken = sessionCookie("alice6-" + UUID.randomUUID());
		String bobToken = sessionCookie("bob6-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Start Game Lobby");

		joinLobby(bobToken, lobbyId);
		checkStart(aliceToken, lobbyId, "/game/" + lobbyId);
	}

	@Test
	void delete_deletesLobbyAndRedirects() {
		String token = sessionCookie("deleter-" + UUID.randomUUID());
		String lobbyId = createLobby(token, "Delete Me Lobby");

		checkDelete(token, lobbyId, "/lobby");
		checkGetLobby(token, lobbyId).statusCode(303).header("Location", endsWith("/lobby"));
	}

	@Test
	void delete_cannotDeleteStartedLobby() {
		String aliceToken = sessionCookie("alice8-" + UUID.randomUUID());
		String bobToken = sessionCookie("bob8-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Started Delete Lobby");

		joinLobby(bobToken, lobbyId);
		startLobby(aliceToken, lobbyId);
		checkDelete(aliceToken, lobbyId, "/lobby/" + lobbyId);
	}

	@Test
	void status_returnsJson() {
		String token = sessionCookie("status-user-" + UUID.randomUUID());
		String lobbyId = createLobby(token, "Status Lobby");

		checkGetStatus(token, lobbyId)
				.body("id", is(lobbyId))
				.body("status", is("WAITING"))
				.body("members", hasSize(1));
	}

	@Test
	void status_containsRedirectUrlWhenStarted() {
		String aliceToken = sessionCookie("alice7-" + UUID.randomUUID());
		String bobToken = sessionCookie("bob7-" + UUID.randomUUID());
		String lobbyId = createLobby(aliceToken, "Status Started Lobby");

		joinLobby(bobToken, lobbyId);
		startLobby(aliceToken, lobbyId);
		checkGetStatus(aliceToken, lobbyId).body("redirectUrl", is("/game/" + lobbyId));
	}
}
