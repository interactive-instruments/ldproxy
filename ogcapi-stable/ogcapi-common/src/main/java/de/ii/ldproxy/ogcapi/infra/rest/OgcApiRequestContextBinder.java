/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
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


@Component
@Provides
@Instantiate
@Provider
public class OgcApiRequestContextBinder extends AbstractBinder implements Binder, OgcApiRequestInjectableContext {

    public static final String OGCAPI_REQUEST_CONTEXT_KEY = "OGCAPI_REQUEST";

    @Override
    public void inject(ContainerRequestContext containerRequestContext, OgcApiRequestContext ogcApiRequestContext) {
        containerRequestContext.setProperty(OGCAPI_REQUEST_CONTEXT_KEY, ogcApiRequestContext);
    }

    @Override
    protected void configure() {
        bindFactory(OgcApiRequestFactory.class).proxy(true)
                                               .proxyForSameScope(false)
                                               .to(OgcApiRequestContext.class)
                                               .in(RequestScoped.class);

        bindFactory(OgcApiDatasetFactory.class).proxy(true)
                                               .proxyForSameScope(false)
                                               .to(OgcApiApi.class)
                                               .in(RequestScoped.class);
    }

    public static class OgcApiRequestFactory extends AbstractContainerRequestValueFactory<OgcApiRequestContext> {

        @Override
        @RequestScoped
        public OgcApiRequestContext provide() {
            return (OgcApiRequestContext) getContainerRequest().getProperty(OGCAPI_REQUEST_CONTEXT_KEY);
        }
    }

    public static class OgcApiDatasetFactory extends AbstractContainerRequestValueFactory<OgcApiApi> {

        @Override
        @RequestScoped
        public OgcApiApi provide() {
            return (OgcApiApi) getContainerRequest().getProperty(SERVICE_CONTEXT_KEY);
        }
    }
}
