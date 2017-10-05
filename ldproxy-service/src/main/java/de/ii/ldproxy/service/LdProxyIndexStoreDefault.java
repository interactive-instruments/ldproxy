/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.*;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class LdProxyIndexStoreDefault extends AbstractGenericResourceStore<PropertyIndex, LdProxyIndexStore> implements LdProxyIndexStore {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyIndexStoreDefault.class);
    public static final String STORE_ID = "ldproxy-indices";

    public LdProxyIndexStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore) {
        super(rootConfigStore, STORE_ID, jackson.getDefaultObjectMapper(), true);
    }

    // TODO
    @Validate
    private void start() {
        fillCache();
    }

    @Override
    protected PropertyIndex createEmptyResource() {
        return new PropertyIndex();
    }
}
