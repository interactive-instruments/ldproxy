/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.xtraplatform.event.store.AbstractKeyValueStore;
import de.ii.xtraplatform.event.store.EventStore;
import de.ii.xtraplatform.event.store.Identifier;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class StylesStoreImpl extends AbstractKeyValueStore<byte[]> implements StylesStore {

    private static final String EVENT_TYPE = "styles";

    protected StylesStoreImpl(@Requires EventStore eventStore) {
        super(eventStore, EVENT_TYPE);
    }

    @Override
    protected byte[] serialize(byte[] value) {
        return value;
    }

    @Override
    protected byte[] deserialize(Identifier identifier, byte[] payload) {
        return payload;
    }
}
