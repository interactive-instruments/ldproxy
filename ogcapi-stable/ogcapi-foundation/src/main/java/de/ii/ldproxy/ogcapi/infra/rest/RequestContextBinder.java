/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.RequestInjectableContext;
import de.ii.xtraplatform.services.domain.ServiceInjectableContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;


@Component
@Provides
@Instantiate
@Provider
public class RequestContextBinder extends AbstractBinder implements Binder, RequestInjectableContext {

    public static final String OGCAPI_REQUEST_CONTEXT_KEY = "OGCAPI_REQUEST";

    @Override
    public void inject(ContainerRequestContext containerRequestContext, ApiRequestContext apiRequestContext) {
        containerRequestContext.setProperty(OGCAPI_REQUEST_CONTEXT_KEY, apiRequestContext);
    }

    @Override
    protected void configure() {
        bindFactory(OgcApiRequestFactory.class).proxy(true)
                                               .proxyForSameScope(false)
                                               .to(ApiRequestContext.class)
                                               .in(RequestScoped.class);

        bindFactory(OgcApiDatasetFactory.class).proxy(true)
                                               .proxyForSameScope(false)
                                               .to(OgcApi.class)
                                               .in(RequestScoped.class);
    }

    public static class OgcApiRequestFactory extends AbstractContainerRequestValueFactory<ApiRequestContext> {

        @Override
        @RequestScoped
        public ApiRequestContext provide() {
            return (ApiRequestContext) getContainerRequest().getProperty(OGCAPI_REQUEST_CONTEXT_KEY);
        }
    }

    public static class OgcApiDatasetFactory extends AbstractContainerRequestValueFactory<OgcApi> {

        @Override
        @RequestScoped
        public OgcApi provide() {
            return (OgcApi) getContainerRequest().getProperty(ServiceInjectableContext.SERVICE_CONTEXT_KEY);
        }
    }
}
