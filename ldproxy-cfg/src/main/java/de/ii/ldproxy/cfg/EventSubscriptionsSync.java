/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.entities.app.EventSubscriptions;
import de.ii.xtraplatform.entities.domain.EventStoreSubscriber;
import de.ii.xtraplatform.entities.domain.Identifier;
import de.ii.xtraplatform.entities.domain.ImmutableStateChangeEvent;
import de.ii.xtraplatform.entities.domain.ReplayEvent;
import de.ii.xtraplatform.entities.domain.StateChangeEvent;
import de.ii.xtraplatform.entities.domain.TypedEvent;
import de.ii.xtraplatform.streams.domain.Event;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class EventSubscriptionsSync implements EventSubscriptions {

  private final Map<String, List<Event>> eventStreams;
  private final Map<String, List<Identifier>> ignore;
  private boolean isStarted;

  protected EventSubscriptionsSync() {
    this.eventStreams = new ConcurrentHashMap<>();
    this.ignore = new LinkedHashMap<>();
  }

  public void addIgnore(String type, Identifier identifier) {
    List<Identifier> identifiers = ignore.computeIfAbsent(type, k -> new ArrayList<>());
    identifiers.add(identifier);
  }

  @Override
  public void addSubscriber(EventStoreSubscriber subscriber) {
    for (String eventType : subscriber.getEventTypes()) {
      List<Event> eventStream = getEventStream(eventType);
      CompletableFuture<Void> cmp = new CompletableFuture<>();

      eventStream.forEach(
          event -> {
            if (ignore.containsKey(eventType)
                && event instanceof ReplayEvent
                && ignore.get(eventType).contains(((ReplayEvent) event).identifier())) {
              // System.out.println("IGNORE " + ((ReplayEvent) event).identifier());
              cmp.complete(null);

              return;
            }
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
