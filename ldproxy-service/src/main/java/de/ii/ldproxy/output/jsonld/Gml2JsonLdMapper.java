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
package de.ii.ldproxy.output.jsonld;

import de.ii.ldproxy.output.html.Gml2MicrodataMapper;
import de.ii.ogc.wfs.proxy.WfsProxyService;

/**
 * @author zahnen
 */
public class Gml2JsonLdMapper extends Gml2MicrodataMapper {


    public static final String MIME_TYPE = "application/ld+json";

    public Gml2JsonLdMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }
}
