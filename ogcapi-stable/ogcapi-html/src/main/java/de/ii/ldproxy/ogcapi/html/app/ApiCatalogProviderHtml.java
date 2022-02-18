/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiCatalog;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiCatalogProvider;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class ApiCatalogProviderHtml extends ApiCatalogProvider {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    @Inject
    public ApiCatalogProviderHtml(AppContext appContext, I18n i18n, EntityDataDefaultsStore defaultsStore, ExtensionRegistry extensionRegistry) {
        super(appContext, i18n, defaultsStore, extensionRegistry);
    }

    private HtmlConfiguration getHtmlConfig() {
        //TODO: encapsulate in entities/defaults layer
        EntityDataBuilder<?> builder = defaultsStore.getBuilder(Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, Service.TYPE, OgcApiDataV2.SERVICE_TYPE.toLowerCase()));
        if (builder instanceof ImmutableOgcApiDataV2.Builder) {
            ImmutableOgcApiDataV2 defaults = ((ImmutableOgcApiDataV2.Builder) builder).id("NOT_SET")
                                                                                      .build();
            return defaults.getExtension(HtmlConfiguration.class)
                           .orElse(new ImmutableHtmlConfiguration.Builder().build());
        }
        return new ImmutableHtmlConfiguration.Builder().build();
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
                       .entity(new ServiceOverviewView(uri, apiCatalog, getHtmlConfig(), i18n, language))
                       .build();
    }

}
