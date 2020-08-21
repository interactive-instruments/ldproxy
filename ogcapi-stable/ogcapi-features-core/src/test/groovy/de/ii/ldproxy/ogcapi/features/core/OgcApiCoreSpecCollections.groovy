/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core;

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.application.I18nDefault
import de.ii.ldproxy.ogcapi.application.OgcApiEntity
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCollections
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.core.api.ImmutableFeaturesCollectionQueryables
import de.ii.ldproxy.ogcapi.features.core.api.FeaturesCoreProviders
import de.ii.ldproxy.ogcapi.features.core.api.FeatureFormatExtension
import de.ii.ldproxy.ogcapi.features.core.application.CollectionExtensionFeatures
import de.ii.ldproxy.ogcapi.features.core.application.CollectionsExtensionFeatures
import de.ii.ldproxy.ogcapi.features.core.application.ImmutableOgcApiFeaturesCoreConfiguration
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableRequestContext
import de.ii.ldproxy.ogcapi.infra.rest.OgcApiEndpointCollection
import de.ii.ldproxy.ogcapi.infra.rest.OgcApiEndpointCollections
import de.ii.xtraplatform.crs.domain.BoundingBox
import de.ii.xtraplatform.features.domain.FeatureProvider2
import spock.lang.Specification

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.stream.Collectors

class OgcApiCoreSpecCollections extends Specification {

    static final ExtensionRegistry registry = createExtensionRegistry()
    static final OgcApiDataV2 datasetData = createDatasetData()
    static final OgcApiEntity ogcApiApiEntity = createOgcApiApiEntity()
    static final ApiRequestContext requestContext = createRequestContext()
    static OgcApiQueriesHandlerCollections ogcApiQueriesHandlerCollections = new OgcApiQueriesHandlerCollections(registry)
    static final OgcApiEndpointCollections collectionsEndpoint = createCollectionsEndpoint()
    static final OgcApiEndpointCollection collectionEndpoint = createCollectionEndpoint()

    def setupSpec() {
        ogcApiQueriesHandlerCollections.i18n = new I18nDefault()
    }


    def 'Requirement 13 A: collections response'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        def collections = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'the response shall include a link to this document'
        collections.links.any { it.rel == 'self' }

        and: 'a link to the response document in every other media type supported by the server'
        collections.links.any { it.rel == 'alternate' }
    }

    def 'Requirement 14 A: collections response'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        def result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'each feature shall be provided in the property collections'
        result.collections.any { it.id == 'featureType1' }
    }

    def 'Requirement 15 A: collections items links'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        def result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'links property of each feature shall include an item for each supported encoding with a link to the features resource'
        def links = result.getCollections().stream().map { x -> x.getLinks() }.collect(Collectors.toList())
        links.any { it.any { it.rel == "items" } }
    }

    def 'Requirement 15 B: collections items links'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        def result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'all links shall include the rel and type properties'
        def links = result.getCollections()
                .stream()
                .map { x -> x.getLinks() }
                .collect(Collectors.toList())
        links.any { it.any { it.rel == "items" && it.type == "application/json" } }
    }

    def 'Requirement 16 A: extent property'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Collections result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'the extent property shall provide bounding boxes that include all spatial geometries and time intervals that include all temporal geometries in this collection'
        result.collections.any { it.extent.get().getSpatial().bbox[0] == ([-180.0, -90.0, 180.0, 90.0] as double[]) }
    }


    def 'Requirement 18 B: feature collection response'() {
        given: 'A request to the server at /collections'

        when: 'the response is created'
        def allCollections = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections
        def singleCollection = collectionEndpoint.getCollection(Optional.empty(), ogcApiApiEntity, requestContext, "featureType1").entity as OgcApiCollection

        then: 'the values for id, title, description and extent shall be identical to the values in the /collections response'
        allCollections.getCollections().get(0).id == singleCollection.id
        allCollections.getCollections().get(0).title == singleCollection.title
        allCollections.getCollections().get(0).description == singleCollection.description
        allCollections.getCollections().get(0).extent.get().getSpatial().bbox == singleCollection.getExtent().get().getSpatial().bbox
        allCollections.getCollections().get(0).extent.get().getSpatial().crs == singleCollection.extent.get().getSpatial().crs
        allCollections.getCollections().get(0).extent.get().getTemporal().interval == singleCollection.extent.get().getTemporal().interval
        allCollections.getCollections().get(0).extent.get().getTemporal().trs == singleCollection.extent.get().getTemporal().trs
    }


    static def createExtensionRegistry() {
        new ExtensionRegistry() {

            @Override
            List<ApiExtension> getExtensions() {
                return ImmutableList.of()

            }

            @Override
            <T extends ApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
                if (extensionType == CollectionsFormatExtension.class) {
                    return ImmutableList.of((T) new CollectionsFormatExtension() {
                        @Override
                        ApiMediaType getMediaType() {
                            return new ImmutableApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        Response getCollectionsResponse(Collections collections, OgcApi api, ApiRequestContext requestContext) {
                            return Response.ok().entity(collections).build()
                        }

                        @Override
                        Response getCollectionResponse(OgcApiCollection ogcApiCollection, OgcApi api, ApiRequestContext requestContext) {
                            return Response.ok().entity(ogcApiCollection).build()
                        }

                    })
                }
                if (extensionType == OgcApiCollectionsExtension.class) {
                    return ImmutableList.of((T) new CollectionsExtensionFeatures(registry))
                }
                if (extensionType == OgcApiCollectionExtension.class) {
                    FeaturesCoreProviders providers = new FeaturesCoreProviders() {
                        @Override
                        FeatureProvider2 getFeatureProvider(OgcApiDataV2 apiData) {
                            return null
                        }

                        @Override
                        FeatureProvider2 getFeatureProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
                            return null
                        }
                    }
                    CollectionExtensionFeatures collectionExtension = new CollectionExtensionFeatures(registry, providers)
                    collectionExtension.i18n = new I18nDefault()
                    return ImmutableList.of((T) collectionExtension)
                }
                if (extensionType == FeatureFormatExtension.class) {
                    return ImmutableList.of((T) new FeatureFormatExtension() {

                        @Override
                        ApiMediaType getMediaType() {
                            return new ImmutableApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDataV2 apiData) {
                            return true
                        }

                        @Override
                        ApiMediaType getCollectionMediaType() {
                            return new ImmutableApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }
                    })
                }
                return ImmutableList.of()
            }
        }
    }

    static def createRequestContext(String uri = 'http://example.com/collections') {
        new ImmutableRequestContext.Builder()
                .mediaType(new ImmutableApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .alternateMediaTypes(ImmutableList.of(new ImmutableApiMediaType.Builder()
                        .type(MediaType.APPLICATION_XML_TYPE)
                        .build()))
                .api(ogcApiApiEntity)
                .requestUri(new URI(uri))
                .language(Locale.GERMAN)
                .build()
    }

    static def createDatasetData() {
        return new ImmutableOgcApiDataV2.Builder()
                .id('test')
                .serviceType('WFS3')
                .putCollections('featureType1', new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                        .id('featureType1')
                        .label('FeatureType 1')
                        .description('foo bar')
                        .extent(new ImmutableCollectionExtent.Builder()
                                .spatial(new BoundingBox())
                                .temporal(new ImmutableTemporalExtent.Builder().build())
                                .build())
                        .addExtensions(new ImmutableOgcApiFeaturesCoreConfiguration.Builder()
                                .queryables(new ImmutableFeaturesCollectionQueryables.Builder()
                                        .spatial(ImmutableList.of('geometry'))
                                        .temporal(ImmutableList.of('datum_open'))
                                        .build())
                                .build())
                        .build())
                .addExtensions(new ImmutableOgcApiFeaturesCoreConfiguration.Builder().build())
                .build()
    }


    static def createOgcApiApiEntity() {
        def entity = new OgcApiEntity(registry)
        entity.setData(datasetData)
        return entity
    }

    static def createCollectionsEndpoint() {
        def endpoint = new OgcApiEndpointCollections(registry)
        endpoint.queryHandler = ogcApiQueriesHandlerCollections
        return endpoint
    }

    static def createCollectionEndpoint() {
        def endpoint = new OgcApiEndpointCollection(registry)
        endpoint.queryHandler = ogcApiQueriesHandlerCollections
        return endpoint
    }

}