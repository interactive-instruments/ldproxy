/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class TileSetsFormatHtml implements TileSetsFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final ExtensionRegistry extensionRegistry;
    private final I18n i18n;
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public TileSetsFormatHtml(@Requires ExtensionRegistry extensionRegistry, @Requires I18n i18n,
                              @Requires CollectionDynamicMetadataRegistry metadataRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
        this.metadataRegistry = metadataRegistry;
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

        HtmlConfiguration htmlConfig = collectionId.isPresent() ?
                                            api.getData()
                                                 .getCollections()
                                                 .get(collectionId.get())
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null) :
                                            api.getData()
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        Map<String, TileMatrixSet> tileMatrixSets = extensionRegistry.getExtensionsForType(TileMatrixSet.class)
                                                                     .stream()
                                                                     .collect(Collectors.toMap(TileMatrixSet::getId, tms -> tms));

        Optional<BoundingBox> optionalBbox = collectionId.isEmpty() ? metadataRegistry.getSpatialExtent(api.getId()) : metadataRegistry.getSpatialExtent(api.getId(), collectionId.get());
        Optional<TemporalExtent> optionalInterval = collectionId.isEmpty() ? metadataRegistry.getTemporalExtent(api.getId()) : metadataRegistry.getTemporalExtent(api.getId(), collectionId.get());
        return new TileSetsView(api.getData(), collectionId, optionalBbox.orElse(null), optionalInterval.orElse(null), tiles, tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
