/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.html;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiErrorMessage;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ExceptionFormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind(interfaces = {ExceptionFormatExtension.class, FormatExtension.class, ApiExtension.class}) //TODO: workaround for issue in dagger-auto
public class ExceptionFormatHtml extends ErrorEntityWriter<ApiErrorMessage, OgcApiErrorView> implements ExceptionFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .build();

    @Inject
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
