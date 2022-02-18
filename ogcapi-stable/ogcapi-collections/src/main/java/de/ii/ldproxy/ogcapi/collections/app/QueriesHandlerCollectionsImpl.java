/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsExtension;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.collections.domain.QueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.Metadata;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryInput;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AutoBind
public class QueriesHandlerCollectionsImpl implements QueriesHandlerCollections {

    private final I18n i18n;

    public enum Query implements QueryIdentifier {COLLECTIONS, FEATURE_COLLECTION}

    @Value.Immutable
    public interface QueryInputCollections extends QueryInput {
        boolean getIncludeLinkHeader();
        List<Link> getAdditionalLinks();
    }

    @Value.Immutable
    public interface QueryInputFeatureCollection extends QueryInput {
        String getCollectionId();
        boolean getIncludeLinkHeader();
        List<Link> getAdditionalLinks();
    }

    private final ExtensionRegistry extensionRegistry;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    @Inject
    public QueriesHandlerCollectionsImpl(ExtensionRegistry extensionRegistry, I18n i18n) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
        this.queryHandlers = ImmutableMap.of(
                Query.COLLECTIONS, QueryHandler.with(QueryInputCollections.class, this::getCollectionsResponse),
                Query.FEATURE_COLLECTION, QueryHandler.with(QueryInputFeatureCollection.class, this::getCollectionResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getCollectionsResponse(QueryInputCollections queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();

        Optional<String> licenseUrl = apiData.getMetadata().flatMap(Metadata::getLicenseUrl);
        Optional<String> licenseName = apiData.getMetadata().flatMap(Metadata::getLicenseName);
        List<Link> links = new CollectionsLinksGenerator()
                .generateLinks(requestContext.getUriCustomizer()
                                             .copy(),
                        Optional.empty(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        licenseUrl,
                        licenseName,
                        i18n,
                        requestContext.getLanguage());

        ImmutableCollections.Builder collections = ImmutableCollections.builder()
                .title(apiData.getLabel())
                .description(apiData.getDescription().orElse(""))
                .links(links)
                .addAllLinks(queryInput.getAdditionalLinks());

        for (CollectionsExtension ogcApiCollectionsExtension : getCollectionsExtenders()) {
            collections = ogcApiCollectionsExtension.process(collections,
                    apiData,
                    requestContext.getUriCustomizer()
                                  .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        CollectionsFormatExtension outputFormatExtension = api.getOutputFormat(CollectionsFormatExtension.class,
                                                                               requestContext.getMediaType(),
                                                                         "/collections",
                                                                               Optional.empty())
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        Collections responseObject = collections.build();

        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        EntityTag etag = !outputFormatExtension.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || api.getData().getExtension(HtmlConfiguration.class).map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(responseObject, Collections.FUNNEL, outputFormatExtension)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? responseObject.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("collections.%s", outputFormatExtension.getMediaType().fileExtension()))
                .entity(outputFormatExtension.getCollectionsEntity(responseObject, requestContext.getApi(), requestContext))
                .build();

    }

    private Response getCollectionResponse(QueryInputFeatureCollection queryInput,
                                           ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();

        Optional<String> licenseUrl = apiData.getMetadata().flatMap(Metadata::getLicenseUrl);
        Optional<String> licenseName = apiData.getMetadata().flatMap(Metadata::getLicenseName);
        List<Link> links = new CollectionLinksGenerator().generateLinks(
                requestContext.getUriCustomizer()
                    .copy(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                licenseUrl,
                licenseName,
                i18n,
                requestContext.getLanguage());

        CollectionsFormatExtension outputFormatExtension = api.getOutputFormat(CollectionsFormatExtension.class, requestContext.getMediaType(), "/collections/"+collectionId, Optional.of(collectionId))
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ImmutableOgcApiCollection.Builder ogcApiCollection = ImmutableOgcApiCollection.builder()
                .id(collectionId)
                .links(links)
                .addAllLinks(queryInput.getAdditionalLinks());

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = apiData.getCollections()
                                                                         .get(collectionId);
        for (CollectionExtension ogcApiCollectionExtension : getCollectionExtenders()) {
            ogcApiCollection = ogcApiCollectionExtension.process(ogcApiCollection,
                    featureTypeConfiguration,
                    apiData,
                    requestContext.getUriCustomizer()
                                  .copy(),
                    false,
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        OgcApiCollection responseObject = ogcApiCollection.build();

        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        EntityTag etag = !outputFormatExtension.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || api.getData().getExtension(HtmlConfiguration.class, collectionId).map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(responseObject, OgcApiCollection.FUNNEL, outputFormatExtension)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? responseObject.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.%s", collectionId, outputFormatExtension.getMediaType().fileExtension()))
                .entity(outputFormatExtension.getCollectionEntity(responseObject, api, requestContext))
                .build();
    }

    private List<CollectionExtension> getCollectionExtenders() {
        return extensionRegistry.getExtensionsForType(CollectionExtension.class);
    }

    private List<CollectionsExtension> getCollectionsExtenders() {
        return extensionRegistry.getExtensionsForType(CollectionsExtension.class);
    }

    private void addLinks(Response.ResponseBuilder response, ImmutableList<Link> links) {
        links.stream().forEach(link -> response.links(link.getLink()));
    }
}
