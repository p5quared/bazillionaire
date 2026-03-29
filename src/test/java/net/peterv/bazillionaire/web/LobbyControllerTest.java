package net.peterv.bazillionaire.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.UUID;
import net.peterv.bazillionaire.game.TestCreateGameUseCase;
import net.peterv.bazillionaire.services.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LobbyControllerTest {

  @Inject AuthService authService;

  @BeforeEach
  void resetCreateGameUseCase() {
    TestCreateGameUseCase.reset();
  }

  private String sessionCookie() {
    return sessionCookie("player-" + UUID.randomUUID());
  }

  private String sessionCookie(String username) {
    var result = (AuthService.CreateSessionResult.Success) authService.createSession(username);
    return result.token();
  }

  /** Creates a lobby and returns its 6-char ID extracted from the Location header. */
  private String createLobby(String token, String name) {
    return createLobby(token, name, 8);
  }

  private String createLobby(String token, String name, int maxPlayers) {
    var response =
        given()
            .cookie("SESSION_TOKEN", token)
            .formParam("name", name)
            .formParam("maxPlayers", Integer.toString(maxPlayers))
            .redirects()
            .follow(false)
            .when()
            .post("/lobby")
            .then()
            .statusCode(303)
            .extract()
            .response();
    String location = response.getHeader("Location");
    return location.substring(location.lastIndexOf('/') + 1);
  }

  private void postAction(String token, String lobbyId, String action) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/" + action);
  }

  private void checkPost(String token, String lobbyId, String action, String locationSuffix) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/" + action)
        .then()
        .statusCode(303)
        .header("Location", endsWith(locationSuffix));
  }

  private void updateSettings(
      String token,
      String lobbyId,
      int tickerCount,
      int initialBalance,
      int initialPrice,
      int gameDuration) {
    given()
        .cookie("SESSION_TOKEN", token)
        .formParam("tickerCount", String.valueOf(tickerCount))
        .formParam("initialBalance", String.valueOf(initialBalance))
        .formParam("initialPrice", String.valueOf(initialPrice))
        .formParam("gameDuration", String.valueOf(gameDuration))
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/settings");
  }

  private ValidatableResponse checkGetLobby(String token, String lobbyId) {
    return given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .get("/lobby/" + lobbyId)
        .then();
  }

  private ValidatableResponse checkGetStatus(String token, String lobbyId) {
    return given()
        .cookie("SESSION_TOKEN", token)
        .when()
        .get("/lobby/" + lobbyId + "/status")
        .then()
        .statusCode(200)
        .contentType(containsString("application/json"));
  }

  @Test
  void list_requiresAuth() {
    given()
        .redirects()
        .follow(false)
        .when()
        .get("/lobby")
        .then()
        .statusCode(303)
        .header("Location", endsWith("/login"));
  }

  @Test
  void list_showsLobbiesPage() {
    String token = sessionCookie();
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
    String token = sessionCookie();
    given()
        .cookie("SESSION_TOKEN", token)
        .formParam("name", "Test Lobby")
        .formParam("maxPlayers", "4")
        .redirects()
        .follow(false)
        .when()
        .post("/lobby")
        .then()
        .statusCode(303)
        .header("Location", matchesRegex(".*/lobby/[A-Z0-9]{6}$"));
  }

  @Test
  void create_requiresName() {
    String token = sessionCookie();
    given()
        .cookie("SESSION_TOKEN", token)
        .formParam("name", "")
        .redirects()
        .follow(false)
        .when()
        .post("/lobby")
        .then()
        .statusCode(303)
        .header("Location", endsWith("/lobby"));
  }

  @Test
  void detail_showsWaitingRoom() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Detail Test Lobby");
    checkGetLobby(token, lobbyId).statusCode(200).body(containsString("Detail Test Lobby"));
  }

  @Test
  void detail_showsActionsOnlyToLobbyMembers() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Member Actions Lobby");

    checkGetLobby(aliceToken, lobbyId)
        .statusCode(200)
        .body(containsString("Start Game"))
        .body(containsString("Delete Lobby"))
        .body(containsString("Leave"));
    checkGetLobby(bobToken, lobbyId)
        .statusCode(200)
        .body(containsString("Join Lobby"))
        .body(not(containsString("Start Game")))
        .body(not(containsString("Delete Lobby")))
        .body(not(containsString("Leave")));
  }

  @Test
  void detail_redirectsIfStarted() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Started Lobby");

    postAction(bobToken, lobbyId, "join");
    checkPost(aliceToken, lobbyId, "start", "/game/" + lobbyId);
    checkGetLobby(aliceToken, lobbyId)
        .statusCode(303)
        .header("Location", endsWith("/game/" + lobbyId));
  }

  @Test
  void join_addsPlayer() {
    String aliceToken = sessionCookie();
    String bobUsername = "bob-" + UUID.randomUUID();
    String bobToken = sessionCookie(bobUsername);
    String lobbyId = createLobby(aliceToken, "Join Test Lobby");

    checkPost(bobToken, lobbyId, "join", "/lobby/" + lobbyId);
    checkGetLobby(aliceToken, lobbyId).statusCode(200).body(containsString(bobUsername));
  }

  @Test
  void join_isIdempotentForExistingMember() {
    String aliceToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Idempotent Lobby");

    checkPost(aliceToken, lobbyId, "join", "/lobby/" + lobbyId);
  }

  @Test
  void join_rejectsFullLobby() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Full Lobby", 1);

    checkPost(bobToken, lobbyId, "join", "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId).body("status", is("WAITING")).body("members", hasSize(1));
  }

  @Test
  void join_rejectsStartedLobby() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Started Join Lobby");

    checkPost(aliceToken, lobbyId, "start", "/game/" + lobbyId);
    checkPost(bobToken, lobbyId, "join", "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId).body("status", is("STARTED")).body("members", hasSize(1));
  }

  @Test
  void join_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie();
    checkPost(token, "MISSING", "join", "/lobby");
  }

  @Test
  void leave_removesPlayer() {
    String aliceToken = sessionCookie();
    String bobUsername = "bob-" + UUID.randomUUID();
    String bobToken = sessionCookie(bobUsername);
    String lobbyId = createLobby(aliceToken, "Leave Test Lobby");

    postAction(bobToken, lobbyId, "join");
    checkPost(bobToken, lobbyId, "leave", "/lobby");
    checkGetLobby(aliceToken, lobbyId).statusCode(200).body(not(containsString(bobUsername)));
  }

  @Test
  void leave_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie();
    checkPost(token, "MISSING", "leave", "/lobby");
  }

  @Test
  void start_allowsSinglePlayerLobby() {
    String aliceToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Min Players Lobby");

    checkPost(aliceToken, lobbyId, "start", "/game/" + lobbyId);
  }

  @Test
  void start_startsGameAndRedirects() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Start Game Lobby");

    postAction(bobToken, lobbyId, "join");
    checkPost(aliceToken, lobbyId, "start", "/game/" + lobbyId);
  }

  @Test
  void start_requiresLobbyMembership() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Protected Start Lobby");

    checkPost(bobToken, lobbyId, "start", "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId)
        .body("status", is("WAITING"))
        .body("redirectUrl", nullValue());
  }

  @Test
  void start_keepsLobbyWaitingWhenGameCreationFails() {
    String aliceToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Failing Start Lobby");

    TestCreateGameUseCase.failNextCreate();
    checkPost(aliceToken, lobbyId, "start", "/lobby/" + lobbyId);

    checkGetStatus(aliceToken, lobbyId)
        .body("status", is("WAITING"))
        .body("redirectUrl", nullValue());
  }

  @Test
  void delete_deletesLobbyAndRedirects() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Delete Me Lobby");

    checkPost(token, lobbyId, "delete", "/lobby");
    checkGetLobby(token, lobbyId).statusCode(303).header("Location", endsWith("/lobby"));
  }

  @Test
  void delete_requiresLobbyMembership() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Protected Delete Lobby");

    checkPost(bobToken, lobbyId, "delete", "/lobby/" + lobbyId);
    checkGetLobby(aliceToken, lobbyId)
        .statusCode(200)
        .body(containsString("Protected Delete Lobby"));
  }

  @Test
  void delete_cannotDeleteStartedLobby() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Started Delete Lobby");

    postAction(bobToken, lobbyId, "join");
    postAction(aliceToken, lobbyId, "start");
    checkPost(aliceToken, lobbyId, "delete", "/lobby/" + lobbyId);
  }

  @Test
  void delete_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie();
    checkPost(token, "MISSING", "delete", "/lobby");
  }

  @Test
  void status_returnsJson() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Status Lobby");

    checkGetStatus(token, lobbyId)
        .body("id", is(lobbyId))
        .body("status", is("WAITING"))
        .body("members", hasSize(1));
  }

  @Test
  void status_containsRedirectUrlWhenStarted() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Status Started Lobby");

    postAction(bobToken, lobbyId, "join");
    postAction(aliceToken, lobbyId, "start");
    checkGetStatus(aliceToken, lobbyId).body("redirectUrl", is("/game/" + lobbyId));
  }

  @Test
  void status_returnsNotFoundPayloadWhenLobbyIsMissing() {
    String token = sessionCookie();
    checkGetStatus(token, "MISSING")
        .body("status", is("NOT_FOUND"))
        .body("members", hasSize(0))
        .body("redirectUrl", nullValue());
  }

  @Test
  void settings_updatesAndRedirects() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Settings Lobby");

    updateSettings(token, lobbyId, 5, 2000, 200, 300);

    checkGetLobby(token, lobbyId)
        .statusCode(200)
        .body(containsString("value=\"5\""))
        .body(containsString("value=\"2000\""))
        .body(containsString("value=\"200\""))
        .body(containsString("value=\"300\""));
  }

  @Test
  void settings_requiresMembership() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Settings Auth Lobby");

    updateSettings(bobToken, lobbyId, 5, 2000, 200, 300);
  }

  @Test
  void detail_showsDefaultSettings() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Default Settings Lobby");

    checkGetLobby(token, lobbyId)
        .statusCode(200)
        .body(containsString("value=\"3\""))
        .body(containsString("value=\"1000\""))
        .body(containsString("value=\"100\""))
        .body(containsString("value=\"600\""));
  }

  @Test
  void detail_nonMemberSeesSettingsReadOnly() {
    String aliceToken = sessionCookie();
    String bobToken = sessionCookie();
    String lobbyId = createLobby(aliceToken, "Read Only Settings Lobby");

    checkGetLobby(bobToken, lobbyId)
        .statusCode(200)
        .body(containsString("Ticker Count: 3"))
        .body(containsString("$1000"))
        .body(containsString("$100"))
        .body(containsString("600s"));
  }

  @Test
  void start_usesPersistedSettings() {
    String token = sessionCookie();
    String lobbyId = createLobby(token, "Persisted Start Lobby");

    updateSettings(token, lobbyId, 3, 500, 50, 120);

    // Start without any settings params
    checkPost(token, lobbyId, "start", "/game/" + lobbyId);
  }
}
