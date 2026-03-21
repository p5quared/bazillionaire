package net.peterv.bazillionaire.game.adapter.out;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.enterprise.inject.Instance;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;
import org.junit.jupiter.api.Test;

class GameEventDispatcherTest {

  @Test
  void dispatchCallsAllListeners() {
    var count = new AtomicInteger(0);
    GameEventListener listener1 =
        new GameEventListener() {
          @Override
          public void onGameEvents(GameId gameId, List<GameMessage> messages) {
            count.incrementAndGet();
          }
        };
    GameEventListener listener2 =
        new GameEventListener() {
          @Override
          public void onGameEvents(GameId gameId, List<GameMessage> messages) {
            count.incrementAndGet();
          }
        };

    var dispatcher = dispatcherWith(listener1, listener2);
    dispatcher.dispatch(new GameId("g1"), List.of());

    assertEquals(2, count.get());
  }

  @Test
  void dispatchContinuesAfterListenerException() {
    var secondCalled = new AtomicBoolean(false);
    GameEventListener failing =
        new GameEventListener() {
          @Override
          public void onGameEvents(GameId gameId, List<GameMessage> messages) {
            throw new RuntimeException("boom");
          }
        };
    GameEventListener healthy =
        new GameEventListener() {
          @Override
          public void onGameEvents(GameId gameId, List<GameMessage> messages) {
            secondCalled.set(true);
          }
        };

    var dispatcher = dispatcherWith(failing, healthy);
    dispatcher.dispatch(new GameId("g1"), List.of());

    assertTrue(secondCalled.get());
  }

  private static GameEventDispatcher dispatcherWith(GameEventListener... listenerArray) {
    var dispatcher = new GameEventDispatcher();
    dispatcher.listeners = stubInstance(List.of(listenerArray));
    return dispatcher;
  }

  @SuppressWarnings("unchecked")
  private static Instance<GameEventListener> stubInstance(List<GameEventListener> list) {
    return new Instance<>() {
      @Override
      public Iterator<GameEventListener> iterator() {
        return list.iterator();
      }

      @Override
      public Instance<GameEventListener> select(java.lang.annotation.Annotation... qualifiers) {
        return this;
      }

      @Override
      public <U extends GameEventListener> Instance<U> select(
          Class<U> subtype, java.lang.annotation.Annotation... qualifiers) {
        return (Instance<U>) this;
      }

      @Override
      public <U extends GameEventListener> Instance<U> select(
          jakarta.enterprise.util.TypeLiteral<U> subtype,
          java.lang.annotation.Annotation... qualifiers) {
        return (Instance<U>) this;
      }

      @Override
      public boolean isUnsatisfied() {
        return list.isEmpty();
      }

      @Override
      public boolean isAmbiguous() {
        return false;
      }

      @Override
      public void destroy(GameEventListener instance) {}

      @Override
      public Handle<GameEventListener> getHandle() {
        return null;
      }

      @Override
      public Iterable<? extends Handle<GameEventListener>> handles() {
        return List.of();
      }

      @Override
      public GameEventListener get() {
        return list.getFirst();
      }

      @Override
      public boolean isResolvable() {
        return !list.isEmpty();
      }
    };
  }
}
