/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.json.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiCatalog;
import de.ii.ogcapi.foundation.domain.ApiCatalogProvider;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class ApiCatalogProviderJson extends ApiCatalogProvider {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Inject
    public ApiCatalogProviderJson(ServicesContext servicesContext, I18n i18n, EntityDataDefaultsStore defaultsStore, ExtensionRegistry extensionRegistry) {
        super(servicesContext, i18n, defaultsStore, extensionRegistry);
    }

    // TODO: move externalUri handling to XtraplatformRequestContext in ServicesResource
    // TODO: derive Wfs3Request from injected XtraplatformRequest

    @Override
    public ApiMediaType getApiMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE.type();
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
