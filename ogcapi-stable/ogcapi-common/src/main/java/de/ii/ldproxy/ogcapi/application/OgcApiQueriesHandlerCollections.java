/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueriesHandlerCollections.class})
public class OgcApiQueriesHandlerCollections implements OgcApiQueriesHandler<OgcApiQueriesHandlerCollections.Query> {

    @Requires
    I18n i18n;

    public enum Query implements OgcApiQueryIdentifier {COLLECTIONS, FEATURE_COLLECTION}

    @Value.Immutable
    public interface OgcApiQueryInputCollections extends OgcApiQueryInput {
        boolean getIncludeHomeLink();
        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    public interface OgcApiQueryInputFeatureCollection extends OgcApiQueryInput {
        String getCollectionId();
        boolean getIncludeHomeLink();
        boolean getIncludeLinkHeader();
    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;


    public OgcApiQueriesHandlerCollections(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.COLLECTIONS, OgcApiQueryHandler.with(OgcApiQueryInputCollections.class, this::getCollectionsResponse),
                Query.FEATURE_COLLECTION, OgcApiQueryHandler.with(OgcApiQueryInputFeatureCollection.class, this::getCollectionResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getCollectionsResponse(OgcApiQueryInputCollections queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();

        Optional<String> licenseUrl = apiData.getMetadata().flatMap(Metadata::getLicenseUrl);
        Optional<String> licenseName = apiData.getMetadata().flatMap(Metadata::getLicenseName);
        List<OgcApiLink> ogcApiLinks = new CollectionsLinksGenerator()
                .generateLinks(requestContext.getUriCustomizer()
                                             .copy(),
                        Optional.empty(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        licenseUrl,
                        licenseName,
                        queryInput.getIncludeHomeLink(),
                        i18n,
                        requestContext.getLanguage());

        ImmutableCollections.Builder collections = new ImmutableCollections.Builder()
                .title(apiData.getLabel())
                .description(apiData.getDescription().orElse(""))
                .links(ogcApiLinks);

        for (OgcApiCollectionsExtension ogcApiCollectionsExtension : getCollectionsExtenders()) {
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
                                                                         "/collections")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ImmutableCollections responseObject = collections.build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? responseObject.getLinks() : null)
                .entity(outputFormatExtension.getCollectionsEntity(responseObject, requestContext.getApi(), requestContext))
                .build();

    }

    private Response getCollectionResponse(OgcApiQueryInputFeatureCollection queryInput,
                                           OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();

        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }

        Optional<String> licenseUrl = apiData.getMetadata().flatMap(Metadata::getLicenseUrl);
        Optional<String> licenseName = apiData.getMetadata().flatMap(Metadata::getLicenseName);
        List<OgcApiLink> ogcApiLinks = new CollectionLinksGenerator().generateLinks(
                requestContext.getUriCustomizer()
                    .copy(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                licenseUrl,
                licenseName,
                queryInput.getIncludeHomeLink(),
                i18n,
                requestContext.getLanguage());

        CollectionsFormatExtension outputFormatExtension = api.getOutputFormat(CollectionsFormatExtension.class, requestContext.getMediaType(), "/collections/"+collectionId)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ImmutableOgcApiCollection.Builder ogcApiCollection = ImmutableOgcApiCollection.builder()
                .id(collectionId)
                .links(ogcApiLinks);

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = apiData.getCollections()
                                                                         .get(collectionId);
        for (OgcApiCollectionExtension ogcApiCollectionExtension : getCollectionExtenders()) {
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

        ImmutableOgcApiCollection responseObject = ogcApiCollection.build();
        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? responseObject.getLinks() : null)
                .entity(outputFormatExtension.getCollectionEntity(ogcApiCollection.build(), api, requestContext))
                .build();
    }

    private List<OgcApiCollectionExtension> getCollectionExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiCollectionExtension.class);
    }

    private List<OgcApiCollectionsExtension> getCollectionsExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiCollectionsExtension.class);
    }

    private void addLinks(Response.ResponseBuilder response, ImmutableList<OgcApiLink> links) {
        links.stream().forEach(link -> response.links(link.getLink()));
    }
}
