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
import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Provides
@Instantiate
public class OgcApiCommonOutputFormatHtml implements CollectionsFormatExtension, CommonFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Context
    private BundleContext bc;

    @Requires
    private Dropwizard dropwizard;

    @Requires
    private EntityRegistry entityRegistry;

    @Requires
    private Http http;

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private I18n i18n;

    @Requires
    private OgcApiFeatureCoreProviders providers;

    @Override
    public String getPathPattern() {
        return "^/?(?:conformance|collections(?:/[\\w\\-]+)?)?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    private boolean isNoIndexEnabledForApi(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    private boolean showCollectionDescriptionsInOverview(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getCollectionDescriptionsInOverview)
                .orElse(false);
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage,
                                           OgcApiApi api,
                                           OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 2 : 1)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel()))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiLandingPageView landingPageView = new OgcApiLandingPageView(api.getData(), apiLandingPage, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return landingPageView;
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiApi api, OgcApiRequestContext requestContext)  {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String conformanceDeclarationTitle = i18n.get("conformanceDeclarationTitle", requestContext.getLanguage());

        final URICustomizer uriCustomizer = requestContext.getUriCustomizer();
        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                                       uriCustomizer.copy()
                                                     .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 3 : 2)
                                                     .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                                       uriCustomizer.copy()
                                                    .removeLastPathSegments(1)
                                                    .toString()))
                .add(new NavigationDTO(conformanceDeclarationTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiConformanceDeclarationView ogcApiConformanceDeclarationView =
                new OgcApiConformanceDeclarationView(conformanceDeclaration, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), i18n, requestContext.getLanguage());
        return ogcApiConformanceDeclarationView;
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApiApi api, OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ?  3 : 2)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiCollectionsView collectionsView = new OgcApiCollectionsView(api.getData(), collections, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), showCollectionDescriptionsInOverview(api.getData()), i18n, requestContext.getLanguage(), providers.getFeatureProvider(api.getData()).getData().getDataSourceUrl());

        return collectionsView;
    }


    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                          OgcApiApi api,
                                          OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 4 : 3)
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
