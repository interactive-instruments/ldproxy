/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class StyleMetadataFormatHtml implements StyleMetadataFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    private final I18n i18n;

    public StyleMetadataFormatHtml(@Requires I18n i18n) {
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
    public Object getStyleMetadataEntity(StyleMetadata metadata,
                                         OgcApiDataV2 apiData,
                                         Optional<String> collectionId,
                                         ApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
        String stylesTitle = i18n.get("stylesTitle", requestContext.getLanguage());
        String styleTitle = metadata.getTitle().orElse(metadata.getId().orElse("?"));
        String metadataTitle = i18n.get("metadataTitle", requestContext.getLanguage());

        ImmutableList.Builder<NavigationDTO> breadCrumbBuilder = collectionId.isPresent() ?
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer()
                                                                        .copy()
                                                                        .removeLastPathSegments(apiData.getSubPath().size() + 5)
                                                                        .toString()))
                        .add(new NavigationDTO(apiData.getLabel(), requestContext.getUriCustomizer()
                                                                                 .copy()
                                                                                 .removeLastPathSegments(5)
                                                                                 .toString()))
                        .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer()
                                                                               .copy()
                                                                               .removeLastPathSegments(4)
                                                                               .toString()))
                        .add(new NavigationDTO(apiData.getCollections()
                                                      .get(collectionId.get())
                                                      .getLabel(), requestContext.getUriCustomizer()
                                                                                 .copy()
                                                                                 .removeLastPathSegments(3)
                                                                                 .toString())) :
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle,
                                               requestContext.getUriCustomizer().copy()
                                                             .removeLastPathSegments(apiData.getSubPath().size() + 3)
                                                             .toString()))
                        .add(new NavigationDTO(apiData.getLabel(),
                                               requestContext.getUriCustomizer()
                                                             .copy()
                                                             .removeLastPathSegments(3)
                                                             .toString()));

        final List<NavigationDTO> breadCrumbs = breadCrumbBuilder
                .add(new NavigationDTO(stylesTitle,
                                       requestContext.getUriCustomizer()
                                                     .copy()
                                                     .removeLastPathSegments(2)
                                                     .toString()))
                .add(new NavigationDTO(styleTitle,
                                       requestContext.getUriCustomizer()
                                                     .copy()
                                                     .removeLastPathSegments(1)
                                                     .toString()))
                .add(new NavigationDTO(metadataTitle))
                .build();

        HtmlConfiguration htmlConfig = apiData.getExtension(HtmlConfiguration.class)
                                              .orElse(null);

        return new StyleMetadataView(apiData, metadata, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(apiData), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
