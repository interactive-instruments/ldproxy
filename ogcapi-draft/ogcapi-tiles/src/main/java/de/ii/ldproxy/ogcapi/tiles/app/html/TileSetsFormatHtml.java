/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsView;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

@Component
@Provides
@Instantiate
public class TileSetsFormatHtml implements TileSetsFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final I18n i18n;
    private final XtraPlatform xtraPlatform;
    private final TileMatrixSetRepository tileMatrixSetRepository;

    public TileSetsFormatHtml(@Requires I18n i18n,
                              @Requires XtraPlatform xtraPlatform,
                              @Requires TileMatrixSetRepository tileMatrixSetRepository) {
        this.i18n = i18n;
        this.xtraPlatform = xtraPlatform;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/tiles/?$";
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.equals("/tiles") || path.equals("/collections/{collectionId}/tiles"))
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
        String tilesTitle = i18n.get("tilesTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = collectionId.isPresent() ?
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData()
                                                           .getSubPath()
                                                           .size() + 3)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(3)
                                .toString()))
                        .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(2)
                                .toString()))
                        .add(new NavigationDTO(api.getData().getCollections().get(collectionId.get()).getLabel(), requestContext.getUriCustomizer().copy()
                                                                                                                                .removeLastPathSegments(1)
                                                                                                                                .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build() :
                new ImmutableList.Builder<NavigationDTO>()
                        .add(new NavigationDTO(rootTitle,
                                requestContext.getUriCustomizer().copy()
                                        .removeLastPathSegments(api.getData()
                                                                   .getSubPath()
                                                                   .size() + 1)
                                        .toString()))
                        .add(new NavigationDTO(api.getData().getLabel(),
                                requestContext.getUriCustomizer()
                                        .copy()
                                        .removeLastPathSegments(1)
                                        .toString()))
                        .add(new NavigationDTO(tilesTitle))
                        .build();

        Optional<HtmlConfiguration> htmlConfig = collectionId.isPresent() ?
            api.getData().getExtension(HtmlConfiguration.class, collectionId.get()) :
            api.getData().getExtension(HtmlConfiguration.class);

        Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();

        Optional<TilesConfiguration> tilesConfig = collectionId.isEmpty()
                ? api.getData().getExtension(TilesConfiguration.class)
                : api.getData().getExtension(TilesConfiguration.class, collectionId.get());
        MapClient.Type mapClientType = tilesConfig.map(TilesConfiguration::getMapClientType)
                                                  .orElse(MapClient.Type.MAP_LIBRE);
        String serviceUrl = new URICustomizer(xtraPlatform.getServicesUri()).ensureLastPathSegments(api.getData().getSubPath().toArray(String[]::new)).toString();
        String styleUrl = htmlConfig.map(cfg -> cfg.getStyle(tilesConfig.map(TilesConfiguration::getStyle), collectionId, serviceUrl))
                                    .orElse(null);
        boolean removeZoomLevelConstraints = tilesConfig.map(TilesConfiguration::getRemoveZoomLevelConstraints)
                                                        .orElse(false);

        return new TileSetsView(api.getData(), tiles, collectionId, tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), mapClientType, styleUrl, removeZoomLevelConstraints, htmlConfig.orElseThrow(), isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
