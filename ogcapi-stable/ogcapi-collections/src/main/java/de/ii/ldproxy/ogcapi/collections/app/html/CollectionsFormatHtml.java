/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import io.swagger.v3.oas.models.media.Schema;
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
public class CollectionsFormatHtml implements CollectionsFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Requires
    private I18n i18n;

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    private boolean showCollectionDescriptionsInOverview(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getCollectionDescriptionsInOverview)
                .orElse(false);
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApi api, ApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getSubPathLength() + 1)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiCollectionsView collectionsView = new OgcApiCollectionsView(api.getData(), collections, breadCrumbs,
                                                                          requestContext.getStaticUrlPrefix(), htmlConfig,
                                                                          isNoIndexEnabledForApi(api.getData()),
                                                                          showCollectionDescriptionsInOverview(api.getData()),
                                                                          i18n, requestContext.getLanguage(),
                                                                          Optional.empty()
                                                                          /* TODO no access to feature providers at this point
                                                                          providers.getFeatureProvider(api.getData()).getData().getDataSourceUrl()
                                                                          */
                                                                          );

        return collectionsView;
    }


    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                      OgcApi api,
                                      ApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getSubPathLength() + 2)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(ogcApiCollection.getTitle().orElse(ogcApiCollection.getId())))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getCollections()
                                                 .get(ogcApiCollection.getId())
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        OgcApiCollectionView collectionView = new OgcApiCollectionView(api.getData(), ogcApiCollection, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), i18n, requestContext.getLanguage());

        return collectionView;
    }
}
