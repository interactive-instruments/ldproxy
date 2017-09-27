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

import de.ii.xsf.configstore.api.rest.ResourceStore;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface LdProxyServiceStore extends ResourceStore<LdProxyService> {
    LdProxyService addService(String id, String wfsUrl) throws IOException;
    LdProxyService addService(String id, String wfsUrl, boolean disableMapping) throws IOException;
}
