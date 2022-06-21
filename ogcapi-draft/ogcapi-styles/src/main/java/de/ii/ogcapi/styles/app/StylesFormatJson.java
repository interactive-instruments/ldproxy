/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.styles.domain.Styles;
import de.ii.ogcapi.styles.domain.StylesFormatExtension;
import io.swagger.v3.oas.models.media.Schema;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class StylesFormatJson implements StylesFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema<?> schemaStyles;
    private final Map<String, Schema<?>> referencedSchemas;

    @Inject
    public StylesFormatJson(ClassSchemaCache classSchemaCache) {
        schemaStyles = classSchemaCache.getSchema(Styles.class);
        referencedSchemas = classSchemaCache.getReferencedSchemas(Styles.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object getStylesEntity(Styles styles, OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext) {
        return styles;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.endsWith("/styles"))
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaStyles)
                .schemaRef(Styles.SCHEMA_REF)
                .referencedSchemas(referencedSchemas)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();

        throw new RuntimeException("Unexpected path: " + path);
    }
}
