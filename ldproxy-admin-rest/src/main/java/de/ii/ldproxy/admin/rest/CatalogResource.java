/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 interactive instruments GmbH
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package de.ii.ldproxy.admin.rest;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xtraplatform.api.MediaTypeCharset;
import de.ii.xtraplatform.ogc.csw.parser.ExtractWFSUrlsFromCSW;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Collection;

/**
 *
 * @author zahnen
 */
@Component
@Provides(specifications = {CatalogResource.class})
@Instantiate
@Path("/catalog/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class CatalogResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogResource.class);


    @GET
    public Collection<String> parseCatalogPost(/*@Auth(required = false) AuthenticatedUser user,*/ @QueryParam("url") String url) {
        LOGGER.debug("CATALOG {}", url);
        ExtractWFSUrlsFromCSW urlsFromCSW = new ExtractWFSUrlsFromCSW(new DefaultHttpClient(), new SMInputFactory(new InputFactoryImpl()));

        return urlsFromCSW.extract(url);
    }

}
