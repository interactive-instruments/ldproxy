/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
