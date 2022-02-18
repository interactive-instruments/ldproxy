/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import io.swagger.v3.oas.models.media.BinarySchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class StyleFormatArcGisDesktopLayer implements StyleFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatArcGisDesktopLayer.class);

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.esri.lyr"))
            .label("ArcGIS")
            .parameter("lyr")
            .build();

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new BinarySchema())
                .schemaRef("#/components/schemas/binary")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new BinarySchema())
                .schemaRef("#/components/schemas/binary")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "lyr";
    }

    @Override
    public String getSpecification() {
        return "https://www.esri.com/";
    }

    @Override
    public String getVersion() {
        return "n/a";
    }
}
