/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
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

    @Requires
    private ExtensionRegistry extensionRegistry;

    @Requires
    private I18n i18n;

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
                                .removeLastPathSegments(api.getData().getSubPathLength() + 3)
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
                                        .removeLastPathSegments(api.getData().getSubPathLength() + 1)
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
                .collect(Collectors.toMap(tms -> tms.getId(), tms -> tms));

        TileSetsView tileSetsView = new TileSetsView(api.getData(), tiles, collectionId, tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return tileSetsView;
    }
}
