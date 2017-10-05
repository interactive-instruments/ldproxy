/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import de.ii.xsf.configstore.api.rest.ResourceStore;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface LdProxyServiceStore extends ResourceStore<LdProxyService> {
    LdProxyService addService(String id, String wfsUrl) throws IOException;
    LdProxyService addService(String id, String wfsUrl, boolean disableMapping) throws IOException;
}
