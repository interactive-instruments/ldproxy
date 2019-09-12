/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;

import static de.ii.xtraplatform.rest.ServiceContextBinder.SERVICE_CONTEXT_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Provider
public class OgcApiRequestContextBinder extends AbstractBinder implements Binder, OgcApiRequestInjectableContext {

    public static final String WFS3_REQUEST_CONTEXT_KEY = "WFS3_REQUEST";

    @Override
    public void inject(ContainerRequestContext containerRequestContext, OgcApiRequestContext wfs3Request) {
        containerRequestContext.setProperty(WFS3_REQUEST_CONTEXT_KEY, wfs3Request);
    }

    @Override
    protected void configure() {
        bindFactory(Wfs3RequestFactory.class).proxy(true)
                                             .proxyForSameScope(false)
                                             .to(OgcApiRequestContext.class)
                                             .in(RequestScoped.class);

        bindFactory(OgcApiDatasetFactory.class).proxy(true)
                                               .proxyForSameScope(false)
                                               .to(OgcApiDataset.class)
                                               .in(RequestScoped.class);
    }

    public static class Wfs3RequestFactory extends AbstractContainerRequestValueFactory<OgcApiRequestContext> {

        @Override
        @RequestScoped
        public OgcApiRequestContext provide() {
            return (OgcApiRequestContext) getContainerRequest().getProperty(WFS3_REQUEST_CONTEXT_KEY);
        }
    }

    public static class OgcApiDatasetFactory extends AbstractContainerRequestValueFactory<OgcApiDataset> {

        @Override
        @RequestScoped
        public OgcApiDataset provide() {
            return (OgcApiDataset) getContainerRequest().getProperty(SERVICE_CONTEXT_KEY);
        }
    }
}
