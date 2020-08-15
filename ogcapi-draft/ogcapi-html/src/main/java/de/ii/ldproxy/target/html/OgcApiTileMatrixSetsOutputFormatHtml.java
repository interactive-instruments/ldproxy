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
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetsFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Provides
@Instantiate
public class OgcApiTileMatrixSetsOutputFormatHtml implements TileMatrixSetsFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    @Requires
    private I18n i18n;

    @Override
    public String getPathPattern() {
        return "^/tileMatrixSets(?:/\\w+)?/?$";
    }

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
    public Object getTileMatrixSetsEntity(TileMatrixSets tileMatrixSets,
                                          OgcApiApi api,
                                          OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 3 : 2)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(tileMatrixSetsTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        OgcApiTileMatrixSetsView tileMatrixSetsView = new OgcApiTileMatrixSetsView(api.getData(), tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return tileMatrixSetsView;
    }

    @Override
    public Object getTileMatrixSetEntity(TileMatrixSetData tileMatrixSet,
                                         OgcApiApi api,
                                         OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());
        String title = tileMatrixSet.getTitle().orElse(tileMatrixSet.getIdentifier());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 4 : 3)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(2)
                                .toString()))
                .add(new NavigationDTO(tileMatrixSetsTitle,
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(title))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiTileMatrixSetView tileMatrixSetView = new OgcApiTileMatrixSetView(api.getData(), tileMatrixSet, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return tileMatrixSetView;
    }
}
