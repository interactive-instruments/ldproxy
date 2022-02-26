/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.store.app.EventSubscriptions;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.TypedEvent;
import java.util.LinkedHashMap;
import java.util.Map;

class EventSubscriptionsMock implements EventSubscriptions {

  private final Map<String, EventStoreSubscriber> subscribers;

  public EventSubscriptionsMock() {
    this.subscribers = new LinkedHashMap<>();
  }

  @Override
  public void addSubscriber(EventStoreSubscriber subscriber) {
    subscriber.getEventTypes().forEach(type -> subscribers.put(type, subscriber));
  }

  @Override
  public void emitEvent(TypedEvent event) {
    if (subscribers.containsKey(event.type())) {
      subscribers.get(event.type()).onEmit(event);
    }
  }

  @Override
  public void startListening() {

  }
}
