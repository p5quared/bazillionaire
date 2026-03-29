package net.peterv.bazillionaire.game.feature;

import static org.junit.jupiter.api.Assertions.*;

import net.peterv.bazillionaire.game.domain.JoinResult;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;

public final class GameAssertions {

  // --- Order Result Assertions ---

  public static OrderResult.Filled assertFilled(UseCaseResult<OrderResult> result) {
    assertInstanceOf(OrderResult.Filled.class, result.result());
    return (OrderResult.Filled) result.result();
  }

  public static void assertRejected(UseCaseResult<OrderResult> result) {
    assertInstanceOf(OrderResult.Rejected.class, result.result());
  }

  public static void assertInvalidOrder(UseCaseResult<OrderResult> result) {
    assertInstanceOf(OrderResult.InvalidOrder.class, result.result());
  }

  // --- Join Result Assertions ---

  public static void assertJoined(UseCaseResult<JoinResult> result) {
    assertInstanceOf(JoinResult.Joined.class, result.result());
  }

  public static void assertAllReady(UseCaseResult<JoinResult> result) {
    assertInstanceOf(JoinResult.AllReady.class, result.result());
  }

  public static void assertAlreadyReady(UseCaseResult<JoinResult> result) {
    assertInstanceOf(JoinResult.AlreadyReady.class, result.result());
  }

  public static void assertGameInProgress(UseCaseResult<JoinResult> result) {
    assertInstanceOf(JoinResult.GameInProgress.class, result.result());
  }

  public static void assertInvalidJoin(UseCaseResult<JoinResult> result) {
    assertInstanceOf(JoinResult.InvalidJoin.class, result.result());
  }

  // --- Event Stream Assertions ---

  public static void assertHasEvent(GameTestHarness h, Class<? extends GameEvent> type) {
    assertFalse(
        h.eventsOfType(type).isEmpty(), "Expected at least one " + type.getSimpleName() + " event");
  }

  public static void assertHasNoEvent(GameTestHarness h, Class<? extends GameEvent> type) {
    assertTrue(
        h.eventsOfType(type).isEmpty(),
        "Expected no " + type.getSimpleName() + " events but found " + h.eventsOfType(type).size());
  }

  public static void assertHasEventSince(
      GameTestHarness h, Class<? extends GameEvent> type, int checkpoint) {
    assertFalse(
        h.eventsOfTypeSince(type, checkpoint).isEmpty(),
        "Expected at least one " + type.getSimpleName() + " event since checkpoint");
  }

  public static void assertHasNoEventSince(
      GameTestHarness h, Class<? extends GameEvent> type, int checkpoint) {
    assertTrue(
        h.eventsOfTypeSince(type, checkpoint).isEmpty(),
        "Expected no "
            + type.getSimpleName()
            + " events since checkpoint but found "
            + h.eventsOfTypeSince(type, checkpoint).size());
  }

  public static void assertHasBroadcast(GameTestHarness h, Class<? extends GameEvent> type) {
    assertFalse(
        h.broadcastEventsOfType(type).isEmpty(),
        "Expected at least one broadcast " + type.getSimpleName() + " event");
  }

  public static void assertHasPrivateEventFor(
      GameTestHarness h, String playerId, Class<? extends GameEvent> type) {
    assertFalse(
        h.privateEventsFor(playerId, type).isEmpty(),
        "Expected at least one " + type.getSimpleName() + " private event for " + playerId);
  }

  // --- Game State Assertions ---

  public static void assertGameFinished(GameTestHarness h) {
    assertHasEvent(h, GameEvent.GameFinished.class);
  }

  public static void assertTickerDelisted(GameTestHarness h) {
    assertHasEvent(h, GameEvent.TickerDelisted.class);
  }

  // --- Audience Assertions on specific messages ---

  public static void assertIsPrivateTo(
      net.peterv.bazillionaire.game.domain.event.GameMessage message, String playerId) {
    assertInstanceOf(Audience.Only.class, message.audience());
    assertEquals(new PlayerId(playerId), ((Audience.Only) message.audience()).playerId());
  }

  public static void assertIsBroadcast(
      net.peterv.bazillionaire.game.domain.event.GameMessage message) {
    assertInstanceOf(Audience.Everyone.class, message.audience());
  }

  private GameAssertions() {}
}
