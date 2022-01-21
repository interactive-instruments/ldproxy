/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.html;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ApiErrorMessage;
import de.ii.ldproxy.ogcapi.domain.ExceptionFormatExtension;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class ExceptionFormatHtml extends ErrorEntityWriter<ApiErrorMessage, OgcApiErrorView> implements ExceptionFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .build();


    public ExceptionFormatHtml() {
        super(MediaType.TEXT_HTML_TYPE, OgcApiErrorView.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new StringSchema().example("<html>...</html>"))
                .schemaRef("#/components/schemas/htmlSchema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getExceptionEntity(ApiErrorMessage errorMessage) {
        return getRepresentation(errorMessage);
    }

    @Override
    protected OgcApiErrorView getRepresentation(ApiErrorMessage errorMessage) {
        return new OgcApiErrorView(errorMessage);
    }
}
