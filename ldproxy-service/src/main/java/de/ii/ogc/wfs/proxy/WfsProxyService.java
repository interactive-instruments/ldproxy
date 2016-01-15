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
package de.ii.ogc.wfs.proxy;

import de.ii.xsf.core.api.Service;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;

import java.util.Map;

/**
 * @author zahnen
 */
public interface WfsProxyService extends Service {
    WFSAdapter getWfsAdapter();

    WFSProxyServiceProperties getServiceProperties();

    Map<String, WfsProxyFeatureType> getFeatureTypes();
}
