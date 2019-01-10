/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Provider
public class Wfs3RequestContextBinder extends AbstractBinder implements Binder, Wfs3RequestInjectableContext {

    public static final String WFS3_REQUEST_CONTEXT_KEY = "WFS3_REQUEST";

    @Override
    public void inject(ContainerRequestContext containerRequestContext, Wfs3RequestContext wfs3Request) {
        containerRequestContext.setProperty(WFS3_REQUEST_CONTEXT_KEY, wfs3Request);
    }

    @Override
    protected void configure() {
        bindFactory(Wfs3RequestFactory.class).proxy(true)
                                             .proxyForSameScope(false)
                                             .to(Wfs3RequestContext.class)
                                             .in(RequestScoped.class);
    }

    public static class Wfs3RequestFactory extends AbstractContainerRequestValueFactory<Wfs3RequestContext> {

        @Override
        @RequestScoped
        public Wfs3RequestContext provide() {
            return (Wfs3RequestContext) getContainerRequest().getProperty(WFS3_REQUEST_CONTEXT_KEY);
        }
    }
}
