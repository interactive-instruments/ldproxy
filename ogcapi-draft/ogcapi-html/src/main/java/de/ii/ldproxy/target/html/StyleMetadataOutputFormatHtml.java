/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.StyleMetadataFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Provides
@Instantiate
public class StyleMetadataOutputFormatHtml implements StyleMetadataFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    @Requires
    private I18n i18n;

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new StringSchema().example("<html>...</html>"))
                .schemaRef("#/components/schemas/htmlSchema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    private boolean isNoIndexEnabledForApi(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Response getStyleMetadataResponse(StyleMetadata metadata,
                                             OgcApiApi api,
                                             OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String stylesTitle = i18n.get("stylesTitle", requestContext.getLanguage());
        String styleTitle = metadata.getTitle().orElse(metadata.getId().orElse("?"));
        String metadataTitle = i18n.get("metadataTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 5 : 4)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(3)
                                .toString()))
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

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        StyleMetadataView metadataView = new StyleMetadataView(api.getData(), metadata, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(metadataView)
                .build();
    }
}
