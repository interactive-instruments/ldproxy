/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.resources.domain.ResourcesFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class ResourcesFormatHtml implements ResourcesFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final I18n i18n;

    @Inject
    public ResourcesFormatHtml(I18n i18n) {
        this.i18n = i18n;
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

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Object getResourcesEntity(Resources resources,
                                     OgcApiDataV2 apiData,
                                     ApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String resourcesTitle = i18n.get("resourcesTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(apiData.getSubPath().size() + 1)
                                .toString()))
                .add(new NavigationDTO(apiData.getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(resourcesTitle))
                .build();

        HtmlConfiguration htmlConfig = apiData.getExtension(HtmlConfiguration.class)
                                              .orElse(null);

        return new ResourcesView(apiData, resources, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(apiData), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
