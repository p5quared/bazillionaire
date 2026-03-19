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

  private void joinLobby(String token, String lobbyId) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/join");
  }

  private void startLobby(String token, String lobbyId) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/start");
  }

  private void checkJoin(String token, String lobbyId, String locationSuffix) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/join")
        .then()
        .statusCode(303)
        .header("Location", endsWith(locationSuffix));
  }

  private void checkLeave(String token, String lobbyId, String locationSuffix) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/leave")
        .then()
        .statusCode(303)
        .header("Location", endsWith(locationSuffix));
  }

  private void checkStart(String token, String lobbyId, String locationSuffix) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/start")
        .then()
        .statusCode(303)
        .header("Location", endsWith(locationSuffix));
  }

  private void checkDelete(String token, String lobbyId, String locationSuffix) {
    given()
        .cookie("SESSION_TOKEN", token)
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/delete")
        .then()
        .statusCode(303)
        .header("Location", endsWith(locationSuffix));
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
    String token = sessionCookie("creator2-" + UUID.randomUUID());
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
    String token = sessionCookie("detail-user-" + UUID.randomUUID());
    String lobbyId = createLobby(token, "Detail Test Lobby");
    checkGetLobby(token, lobbyId).statusCode(200).body(containsString("Detail Test Lobby"));
  }

  @Test
  void detail_showsActionsOnlyToLobbyMembers() {
    String aliceToken = sessionCookie("alice-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-" + UUID.randomUUID());
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
    String aliceToken = sessionCookie("alice-started-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-started-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Started Lobby");

    joinLobby(bobToken, lobbyId);
    checkStart(aliceToken, lobbyId, "/game/" + lobbyId);
    checkGetLobby(aliceToken, lobbyId)
        .statusCode(303)
        .header("Location", endsWith("/game/" + lobbyId));
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
  void join_isIdempotentForExistingMember() {
    String aliceToken = sessionCookie("alice3-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Idempotent Lobby");

    checkJoin(aliceToken, lobbyId, "/lobby/" + lobbyId);
  }

  @Test
  void join_rejectsFullLobby() {
    String aliceToken = sessionCookie("alice-full-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-full-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Full Lobby", 1);

    checkJoin(bobToken, lobbyId, "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId).body("status", is("WAITING")).body("members", hasSize(1));
  }

  @Test
  void join_rejectsStartedLobby() {
    String aliceToken = sessionCookie("alice-start-join-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-start-join-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Started Join Lobby");

    checkStart(aliceToken, lobbyId, "/game/" + lobbyId);
    checkJoin(bobToken, lobbyId, "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId).body("status", is("STARTED")).body("members", hasSize(1));
  }

  @Test
  void join_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie("join-missing-" + UUID.randomUUID());
    checkJoin(token, "MISSING", "/lobby");
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
  void leave_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie("leave-missing-" + UUID.randomUUID());
    checkLeave(token, "MISSING", "/lobby");
  }

  @Test
  void start_allowsSinglePlayerLobby() {
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
  void start_requiresLobbyMembership() {
    String aliceToken = sessionCookie("alice-start-member-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-start-member-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Protected Start Lobby");

    checkStart(bobToken, lobbyId, "/lobby/" + lobbyId);
    checkGetStatus(aliceToken, lobbyId)
        .body("status", is("WAITING"))
        .body("redirectUrl", nullValue());
  }

  @Test
  void start_keepsLobbyWaitingWhenGameCreationFails() {
    String aliceToken = sessionCookie("alice-start-fail-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Failing Start Lobby");

    TestCreateGameUseCase.failNextCreate();
    checkStart(aliceToken, lobbyId, "/lobby/" + lobbyId);

    checkGetStatus(aliceToken, lobbyId)
        .body("status", is("WAITING"))
        .body("redirectUrl", nullValue());
  }

  @Test
  void delete_deletesLobbyAndRedirects() {
    String token = sessionCookie("deleter-" + UUID.randomUUID());
    String lobbyId = createLobby(token, "Delete Me Lobby");

    checkDelete(token, lobbyId, "/lobby");
    checkGetLobby(token, lobbyId).statusCode(303).header("Location", endsWith("/lobby"));
  }

  @Test
  void delete_requiresLobbyMembership() {
    String aliceToken = sessionCookie("alice-delete-member-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-delete-member-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Protected Delete Lobby");

    checkDelete(bobToken, lobbyId, "/lobby/" + lobbyId);
    checkGetLobby(aliceToken, lobbyId)
        .statusCode(200)
        .body(containsString("Protected Delete Lobby"));
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
  void delete_redirectsToLobbyListWhenLobbyIsMissing() {
    String token = sessionCookie("delete-missing-" + UUID.randomUUID());
    checkDelete(token, "MISSING", "/lobby");
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

  @Test
  void status_returnsNotFoundPayloadWhenLobbyIsMissing() {
    String token = sessionCookie("status-missing-" + UUID.randomUUID());
    checkGetStatus(token, "MISSING")
        .body("status", is("NOT_FOUND"))
        .body("members", hasSize(0))
        .body("redirectUrl", nullValue());
  }

  @Test
  void settings_updatesAndRedirects() {
    String token = sessionCookie("settings-user-" + UUID.randomUUID());
    String lobbyId = createLobby(token, "Settings Lobby");

    given()
        .cookie("SESSION_TOKEN", token)
        .formParam("tickerCount", "5")
        .formParam("initialBalance", "2000")
        .formParam("initialPrice", "200")
        .formParam("gameDuration", "300")
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/settings")
        .then()
        .statusCode(303)
        .header("Location", endsWith("/lobby/" + lobbyId));

    checkGetLobby(token, lobbyId)
        .statusCode(200)
        .body(containsString("value=\"5\""))
        .body(containsString("value=\"2000\""))
        .body(containsString("value=\"200\""))
        .body(containsString("value=\"300\""));
  }

  @Test
  void settings_requiresMembership() {
    String aliceToken = sessionCookie("alice-settings-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-settings-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Settings Auth Lobby");

    given()
        .cookie("SESSION_TOKEN", bobToken)
        .formParam("tickerCount", "5")
        .formParam("initialBalance", "2000")
        .formParam("initialPrice", "200")
        .formParam("gameDuration", "300")
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/settings")
        .then()
        .statusCode(303)
        .header("Location", endsWith("/lobby/" + lobbyId));
  }

  @Test
  void detail_showsDefaultSettings() {
    String token = sessionCookie("default-settings-" + UUID.randomUUID());
    String lobbyId = createLobby(token, "Default Settings Lobby");

    checkGetLobby(token, lobbyId)
        .statusCode(200)
        .body(containsString("value=\"2\""))
        .body(containsString("value=\"1000\""))
        .body(containsString("value=\"100\""))
        .body(containsString("value=\"600\""));
  }

  @Test
  void detail_nonMemberSeesSettingsReadOnly() {
    String aliceToken = sessionCookie("alice-readonly-" + UUID.randomUUID());
    String bobToken = sessionCookie("bob-readonly-" + UUID.randomUUID());
    String lobbyId = createLobby(aliceToken, "Read Only Settings Lobby");

    checkGetLobby(bobToken, lobbyId)
        .statusCode(200)
        .body(containsString("Ticker Count: 2"))
        .body(containsString("$1000"))
        .body(containsString("$100"))
        .body(containsString("600s"));
  }

  @Test
  void start_usesPersistedSettings() {
    String token = sessionCookie("persisted-start-" + UUID.randomUUID());
    String lobbyId = createLobby(token, "Persisted Start Lobby");

    // Update settings first
    given()
        .cookie("SESSION_TOKEN", token)
        .formParam("tickerCount", "3")
        .formParam("initialBalance", "500")
        .formParam("initialPrice", "50")
        .formParam("gameDuration", "120")
        .redirects()
        .follow(false)
        .when()
        .post("/lobby/" + lobbyId + "/settings");

    // Start without any settings params
    checkStart(token, lobbyId, "/game/" + lobbyId);
  }
}
