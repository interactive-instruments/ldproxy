/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.vt.TileCollections;
import de.ii.ldproxy.wfs3.vt.TileCollectionsFormatExtension;
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
public class TilesOutputFormatHtml implements TileCollectionsFormatExtension {

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
        return "^/tiles/?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    @Override
    public Response getTileCollectionsResponse(TileCollections tiles,
                                               Optional<String> collectionId,
                                               OgcApiDataset api,
                                               OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
        String tilesTitle = i18n.get("tilesTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = collectionId.isPresent() ?
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(4)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(3)
                                .toString()))
                        .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(2)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getFeatureTypes().get(collectionId.get()).getLabel(), requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(1)
                                .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build() :
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle,
                                requestContext.getUriCustomizer().copy()
                                        .removeLastPathSegments(2)
                                        .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(),
                                requestContext.getUriCustomizer()
                                        .copy()
                                        .removeLastPathSegments(1)
                                        .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build();

        TilesView tilesView = new TilesView(api.getData(), tiles, collectionId, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(tilesView)
                .build();
    }
}
