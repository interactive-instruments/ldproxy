/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.json;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiErrorMessage;
import de.ii.ldproxy.ogcapi.foundation.domain.ExceptionFormatExtension;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

@Component
@Provides
@Instantiate
public class ExceptionFormatJson extends ErrorEntityWriter<ApiErrorMessage, ApiErrorMessage> implements ExceptionFormatExtension {

    private final Schema schema;

    public static final String SCHEMA_REF_EXCEPTIONS = "#/components/schemas/Exceptions";

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.valueOf("application/problem+json"))
            .fileExtension("json")
            .build();

    public ExceptionFormatJson() {
        super(MEDIA_TYPE.type(), ApiErrorMessage.class);
        // cannot yet use SchemaGenerator to generate the schema from the ApiErrorMessage class, so we generate the schema manually
        schema = new ObjectSchema().addProperties("title", new StringSchema())
                                   .addProperties("detail", new StringSchema())
                                   .addProperties("status", new IntegerSchema().minimum(BigDecimal.valueOf(100)))
                                   .addProperties("instance", new StringSchema().format("uri"));
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(SCHEMA_REF_EXCEPTIONS)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getExceptionEntity(ApiErrorMessage errorMessage) {
        return errorMessage;
    }

    @Override
    protected ApiErrorMessage getRepresentation(ApiErrorMessage entity) {
        return entity;
    }
}
