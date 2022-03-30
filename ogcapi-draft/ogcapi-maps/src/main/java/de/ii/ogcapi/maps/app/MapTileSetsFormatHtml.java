/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSetsView;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class MapTileSetsFormatHtml implements TileSetsFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final I18n i18n;
    private final TileMatrixSetRepository tileMatrixSetRepository;

    @Inject
    public MapTileSetsFormatHtml(I18n i18n,
                                 TileMatrixSetRepository tileMatrixSetRepository) {
        this.i18n = i18n;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/map/tiles/?$";
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.equals("/map/tiles") || path.equals("/collections/{collectionId}/map/tiles"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(new StringSchema().example("<html>...</html>"))
                    .schemaRef("#/components/schemas/htmlSchema")
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        return null;
    }

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Object getTileSetsEntity(TileSets tiles,
                                    Optional<String> collectionId,
                                    OgcApi api,
                                    ApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
        String tilesTitle = i18n.get("mapTilesTitle", requestContext.getLanguage());

        URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
        final List<NavigationDTO> breadCrumbs = collectionId.isPresent() ?
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle, resourceUri.copy()
                                .removeLastPathSegments(api.getData()
                                                           .getSubPath()
                                                           .size() + 4)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(), resourceUri.copy()
                                .removeLastPathSegments(4)
                                .toString()))
                        .add(new NavigationDTO(collectionsTitle, resourceUri.copy()
                                .removeLastPathSegments(3)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getCollections().get(collectionId.get()).getLabel(), resourceUri.copy()
                                                                                                                                .removeLastPathSegments(1)
                                                                                                                                .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build() :
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle,
                                resourceUri.copy()
                                        .removeLastPathSegments(api.getData()
                                                                   .getSubPath()
                                                                   .size() + 2)
                                        .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(),
                                resourceUri
                                        .copy()
                                        .removeLastPathSegments(2)
                                        .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build();

        Optional<HtmlConfiguration> htmlConfig = collectionId.isPresent() ?
            api.getData().getExtension(HtmlConfiguration.class, collectionId.get()) :
            api.getData().getExtension(HtmlConfiguration.class);

        Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();

        return new TileSetsView(api.getData(), tiles, collectionId, api.getSpatialExtent(collectionId), api.getTemporalExtent(collectionId), tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), MapClient.Type.MAP_LIBRE, null, false, htmlConfig.orElseThrow(), isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
