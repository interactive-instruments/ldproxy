/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.store.app.EventSubscriptions;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.ImmutableStateChangeEvent;
import de.ii.xtraplatform.store.domain.StateChangeEvent;
import de.ii.xtraplatform.store.domain.TypedEvent;
import de.ii.xtraplatform.streams.domain.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class EventSubscriptionsSync implements EventSubscriptions {

  private final Map<String, List<Event>> eventStreams;
  private boolean isStarted;

  protected EventSubscriptionsSync() {
    this.eventStreams = new ConcurrentHashMap<>();
  }

  @Override
  public void addSubscriber(EventStoreSubscriber subscriber) {
    for (String eventType : subscriber.getEventTypes()) {
      List<Event> eventStream = getEventStream(eventType);
      CompletableFuture<Void> cmp = new CompletableFuture<>();
      eventStream.forEach(
          event -> {
            if (event instanceof StateChangeEvent
                && ((StateChangeEvent) event).state() == StateChangeEvent.STATE.LISTENING) {
              subscriber.onEmit(event);
              cmp.complete(null);

              return;
            }

            // only emit one event at a time
            synchronized (this) {
              subscriber.onEmit(event);
            }
          });
      cmp.join();
    }
    // });
  }

  @Override
  public synchronized void emitEvent(TypedEvent event) {
    final List<Event> eventStream = getEventStream(event.type());
    eventStream.add(event);
  }

  @Override
  public void startListening() {
    eventStreams.forEach(
        (eventType, eventStream) ->
            emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING, eventType));
    this.isStarted = true;
  }

  private synchronized List<Event> getEventStream(String eventType) {
    Objects.requireNonNull(eventType, "eventType may not be null");
    return eventStreams.computeIfAbsent(eventType, prefix -> createEventStream(eventType));
  }

  private List<Event> createEventStream(String eventType) {
    List<Event> eventStream = new ArrayList<>();

    emitStateChange(eventStream, StateChangeEvent.STATE.REPLAYING, eventType);

    // should only happen if there is no replay, so order would be correct
    if (isStarted) {
      emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING, eventType);
    }

    return eventStream;
  }

  private void emitStateChange(List<Event> eventStream, StateChangeEvent.STATE state, String type) {
    eventStream.add(ImmutableStateChangeEvent.builder().state(state).type(type).build());
  }
}