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
package de.ii.ldproxy.rest;

import de.ii.ldproxy.output.html.ServiceOverviewView;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import io.dropwizard.views.View;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import java.net.URI;
import java.util.Collection;

/**
 *
 * @author zahnen
 */
@Component
@Provides(properties= {
            @StaticServiceProperty(name=ServiceResource.SERVICE_TYPE_KEY, type="java.lang.String", value= LdProxyService.SERVICE_TYPE)
    })
@Instantiate

public class LdProxyServiceResourceFactory implements ServiceResourceFactory {

    @Override
    public Class getServiceResourceClass() {
        return LdProxyServiceResource.class;
    }

    @Override
    public View getServicesView(Collection<Service> collection, URI uri) {
        return new ServiceOverviewView(uri, collection);
    }

}
