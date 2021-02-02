/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.AbstractKeyValueStore;
import de.ii.xtraplatform.event.store.EntityDataDefaults;
import de.ii.xtraplatform.event.store.EventSourcing;
import de.ii.xtraplatform.event.store.EventStore;
import de.ii.xtraplatform.event.store.Identifier;
import de.ii.xtraplatform.event.store.ValueEncoding;
import de.ii.xtraplatform.event.store.ValueEncodingJackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class StylesStoreImpl extends AbstractKeyValueStore<byte[]> implements StylesStore {

    private static final String EVENT_TYPE = "styles";

    private final ValueEncoding<byte[]> valueEncoding;
    private final EventSourcing<byte[]> eventSourcing;

    protected StylesStoreImpl(@Requires EventStore eventStore) {
        this.valueEncoding = new ValueEncodingBytes();
        this.eventSourcing = new EventSourcing<>(eventStore, ImmutableList.of(EVENT_TYPE), valueEncoding, this::onStart, Optional.empty());
    }

    @Override
    protected EventSourcing<byte[]> getEventSourcing() {
        return eventSourcing;
    }

    class ValueEncodingBytes implements ValueEncoding<byte[]> {

        @Override
        public FORMAT getDefaultFormat() {
            return FORMAT.UNKNOWN;
        }

        @Override
        public byte[] serialize(byte[] data) {
            return data;
        }

        @Override
        public byte[] serialize(Map<String, Object> data) {
            return null;
        }

        @Override
        public byte[] deserialize(Identifier identifier, byte[] payload, FORMAT format) {
            return payload;
        }

        @Override
        public byte[] nestPayload(byte[] payload, String format, List<String> nestingPath, Optional<EntityDataDefaults.KeyPathAlias> keyPathAlias) throws IOException {
            return payload;
        }
    }
}
