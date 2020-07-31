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
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.tiles.TileSets;
import de.ii.ldproxy.ogcapi.tiles.TileSetsFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class TilesOutputFormatHtml implements TileSetsFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    @Requires
    private I18n i18n;

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), HtmlConfiguration.class);
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
    public Object getTileSetsEntity(TileSets tiles,
                                    Optional<String> collectionId,
                                    OgcApiApi api,
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
                        .add(new NavigationDTO(api.getData().getCollections().get(collectionId.get()).getLabel(), requestContext.getUriCustomizer().copy()
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

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getCollections()
                                                 .get(collectionId)
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        TilesView tilesView = new TilesView(api.getData(), tiles, collectionId, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return tilesView;
    }
}
