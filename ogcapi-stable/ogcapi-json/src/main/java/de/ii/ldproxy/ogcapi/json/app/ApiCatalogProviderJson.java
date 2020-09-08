/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.json.app;

import de.ii.ldproxy.ogcapi.domain.ApiCatalog;
import de.ii.ldproxy.ogcapi.domain.ApiCatalogProvider;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class ApiCatalogProviderJson extends ApiCatalogProvider {

    public ApiCatalogProviderJson(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform, @Requires I18n i18n, @Requires EntityDataDefaultsStore defaultsStore, @Requires ExtensionRegistry extensionRegistry) {
        super(bundleContext, xtraPlatform, i18n, defaultsStore, extensionRegistry);
    }

    // TODO: move externalUri handling to XtraplatformRequestContext in ServicesResource
    // TODO: derive Wfs3Request from injected XtraplatformRequest

    @Override
    public MediaType getMediaType() {
        return MediaType.APPLICATION_JSON_TYPE;
    }

    // TODO: add locale parameter in ServiceListing.getServiceListing() in xtraplatform
    @Override
    public Response getServiceListing(List<ServiceData> apis, URI uri, Optional<Locale> language) throws URISyntaxException {
        ApiCatalog apiCatalog = getCatalog(apis, uri, language);

        // TODO: map in caller
        return Response.ok()
                       .entity(apiCatalog)
                       .build();
    }
}
