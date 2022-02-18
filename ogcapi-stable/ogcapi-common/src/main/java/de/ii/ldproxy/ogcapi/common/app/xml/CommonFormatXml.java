/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.xml;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.ObjectSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CommonFormatXml implements CommonFormatExtension {

    private static final ApiMediaType MEDIA_TYPE = new
            ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    @Inject
    public CommonFormatXml() {
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledByDefault() { return false; }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage, OgcApi api, ApiRequestContext requestContext) {
        String title = requestContext.getApi().getData().getLabel();
        String description = requestContext.getApi().getData().getDescription().orElse(null);
        return new LandingPageXml(apiLandingPage.getLinks(), title, description);
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                       OgcApi api, ApiRequestContext requestContext) {
        return new OgcApiConformanceClassesXml(conformanceDeclaration);
    }
}
