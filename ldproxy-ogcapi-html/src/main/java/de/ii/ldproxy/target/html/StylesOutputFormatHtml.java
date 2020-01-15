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
import de.ii.ldproxy.wfs3.styles.StyleMetadata;
import de.ii.ldproxy.wfs3.styles.Styles;
import de.ii.ldproxy.wfs3.styles.StylesFormatExtension;
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
public class StylesOutputFormatHtml implements StylesFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private I18n i18n;

    @Override
    public String getPathPattern() {
        return "^/styles(?:/\\w+)?/?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    private boolean isNoIndexEnabledForApi(OgcApiDatasetData apiData) {
        return getExtensionConfiguration(apiData, HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Response getStylesResponse(Styles styles,
                                      OgcApiDataset api,
                                      OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String stylesTitle = i18n.get("stylesTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(2)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(stylesTitle))
                .build();

        StylesView stylesView = new StylesView(api.getData(), styles, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(stylesView)
                .build();
    }

    @Override
    public Response getStyleMetadataResponse(StyleMetadata metadata,
                                             OgcApiDataset api,
                                             OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String stylesTitle = i18n.get("stylesTitle", requestContext.getLanguage());
        String styleTitle = metadata.getTitle().orElse(metadata.getId().orElse("?"));
        String metadataTitle = i18n.get("metadataTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(4)
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

        StyleMetadataView metadataView = new StyleMetadataView(api.getData(), metadata, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(metadataView)
                .build();
    }
}
