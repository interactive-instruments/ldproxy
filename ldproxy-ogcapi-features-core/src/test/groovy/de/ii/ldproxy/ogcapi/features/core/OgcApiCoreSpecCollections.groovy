/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core;

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.application.I18n
import de.ii.ldproxy.ogcapi.application.OgcApiApiEntity
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCollections
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension
import de.ii.ldproxy.ogcapi.features.core.application.ImmutableOgcApiFeaturesCoreConfiguration
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCollectionExtension
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCollectionsExtension
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext
import de.ii.ldproxy.ogcapi.infra.rest.OgcApiEndpointCollection
import de.ii.ldproxy.ogcapi.infra.rest.OgcApiEndpointCollections
import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.EpsgCrs
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping
import spock.lang.Ignore
import spock.lang.Specification

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.stream.Collectors

class OgcApiCoreSpecCollections extends Specification {

    static final OgcApiExtensionRegistry registry = createExtensionRegistry()
    static final OgcApiDatasetData datasetData = createDatasetData()
    static final OgcApiApiEntity ogcApiApiEntity = createOgcApiApiEntity()
    static final OgcApiRequestContext requestContext = createRequestContext()
    static OgcApiQueriesHandlerCollections ogcApiQueriesHandlerCollections = new OgcApiQueriesHandlerCollections(registry)
    static final OgcApiEndpointCollections collectionsEndpoint = createCollectionsEndpoint()
    static final OgcApiEndpointCollection collectionEndpoint = createCollectionEndpoint()

    def setupSpec() {
        ogcApiQueriesHandlerCollections.i18n = new I18n()
    }

    @Ignore
    def 'Requirement 11 A: GET support at /collections'() {
        when: 'HTTP GET request at /collections'
        then: 'the server returns a response'
    }

    @Ignore
    def 'Requirement 12 A: response code 200'() {
        when: 'HTTP GET request at /collections'
        then: 'the server responds with HTTP status code 200'
    }

    @Ignore
    def 'Requirement 12 B: collections response schema'() {
        when: 'HTTP GET request at /collections'
        then: 'the content of the response conforms to OpenAPI 3.0 schema'
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
        def links = result.getCollections().stream().map{x -> x.getLinks()}.collect(Collectors.toList())
        links.any { it.any { it.rel == "items" } }
    }

    def 'Requirement 15 B: collections items links'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        def result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'all links shall include the rel and type properties'
        def links = result.getCollections()
                .stream()
                .map{x -> x.getLinks()}
                .collect(Collectors.toList())
        links.any { it.any { it.rel == "items" && it.type == "application/json"} }
    }

    def 'Requirement 16 A: extent property'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Collections result = collectionsEndpoint.getCollections(Optional.empty(), ogcApiApiEntity, requestContext).entity as Collections

        then: 'the extent property shall provide bounding boxes that include all spatial geometries and time intervals that include all temporal geometries in this collection'
        result.collections.any { it.extent.get().getSpatial().bbox[0] == ([-180.0, -90.0, 180.0, 90.0] as double[])}
    }

    @Ignore
    def 'Requirement 17 A: GET support at /collections/{collectionId}'() {
        when: 'HTTP GET request at /collections/{collectionId}'
        then: 'the server returns a response'
    }

    @Ignore
    def 'Requirement 17 B: feature collection request: {collectionId} parameter'() {
        when: 'HTTP GET request at /collections/{collectionId}'
        then: 'the parameter {collectionId} is each id property in the feature collections response'
    }

    @Ignore
    def 'Requirement 18 A: feature collection response code 200'() {
        when: 'HTTP GET request at /collections/{collectionId}'
        then: 'the server responds with HTTP status code 200'
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

    @Ignore
    def 'Requirement 19 A: GET support at /collections/{collectionId}/items'() {
        when: 'HTTP GET request at /collections/{collectionId}/items'
        then: 'the server returns a response'
    }

    @Ignore
    def 'Requirement 19 B: {collectionId} parameter'() {
        when: 'HTTP GET request at /collections/{collectionId}/items'
        then: 'the parameter {collectionId} is each id property in the feature collections response'
    }

    @Ignore
    def 'Requirement 20 A: limit parameter support'() {
        when: 'A request at /collections/{collectionId}/items?limit={num} where {num} is an integer between 1 and 10000'
        then: 'the server returns a response'
    }


    static def createExtensionRegistry() {
        new OgcApiExtensionRegistry() {

            @Override
            List<OgcApiExtension> getExtensions() {
                return ImmutableList.of()

            }

            @Override
            <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
                if (extensionType == CollectionsFormatExtension.class) {
                    return ImmutableList.of((T) new CollectionsFormatExtension() {
                        @Override
                        OgcApiMediaType getMediaType() {
                            return new ImmutableOgcApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        Response getCollectionsResponse(Collections collections, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(collections).build()
                        }

                        @Override
                        Response getCollectionResponse(OgcApiCollection ogcApiCollection, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(ogcApiCollection).build()
                        }

                    })
                }
                if (extensionType == OgcApiCollectionsExtension.class) {
                    return ImmutableList.of((T) new OgcApiFeaturesCollectionsExtension(registry))
                }
                if (extensionType == OgcApiCollectionExtension.class) {
                    OgcApiFeaturesCollectionExtension collectionExtension = new OgcApiFeaturesCollectionExtension(registry)
                    collectionExtension.i18n = new I18n()
                    return ImmutableList.of((T) collectionExtension)
                }
                if (extensionType == OgcApiFeatureFormatExtension.class) {
                    return ImmutableList.of((T) new OgcApiFeatureFormatExtension() {

                        @Override
                        OgcApiMediaType getMediaType() {
                            return new ImmutableOgcApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDatasetData apiData) {
                            return true
                        }
                    })
                }
                return ImmutableList.of()
            }
        }
    }

    static def createRequestContext(String uri = 'http://example.com/collections') {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .alternateMediaTypes(ImmutableList.of(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_XML_TYPE)
                        .build()))
                .api(ogcApiApiEntity)
                .requestUri(new URI(uri))
                .language(Locale.GERMAN)
                .build()
    }

    static def createDatasetData() {
        OgcApiFeaturesGenericMapping spatial = new OgcApiFeaturesGenericMapping()
        spatial.type = OgcApiFeaturesGenericMapping.GENERIC_TYPE.SPATIAL
        spatial.name = "geometry"
        spatial.filterable = true

        OgcApiFeaturesGenericMapping temporal = new OgcApiFeaturesGenericMapping()
        temporal.type = OgcApiFeaturesGenericMapping.GENERIC_TYPE.TEMPORAL
        temporal.name = "datum_open"
        temporal.filterable = true

        return new ImmutableOgcApiDatasetData.Builder()
                .id('test')
                .serviceType('WFS3')
                .featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                        .providerType('WFS')
                        .nativeCrs(new EpsgCrs())
                        .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                .uri(new URI('http://example.com'))
                                .method(ConnectionInfoWfsHttp.METHOD.GET)
                                .version('2.0.0')
                                .gmlVersion('3.2.1')
                                .build())
                        .putMappings("featureType1", new ImmutableFeatureTypeMapping.Builder()
                                .putMappings("http://example.com/featureType1:datum_open", new ImmutableSourcePathMapping.Builder()
                                        .putMappings("general", temporal)
                                        .build())
                                .putMappings("http://example.com/featureType1:geometry", new ImmutableSourcePathMapping.Builder()
                                        .putMappings("general", spatial)
                                        .build())
                                .build())
                        .build())
                .putFeatureTypes('featureType1', new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                        .id('featureType1')
                        .label('FeatureType 1')
                        .description('foo bar')
                        .extent(new ImmutableCollectionExtent.Builder()
                                .spatial(new BoundingBox())
                                .temporal(new ImmutableTemporalExtent.Builder().build())
                                .build())
                        .build())
                .addCapabilities(new ImmutableOgcApiFeaturesCoreConfiguration.Builder().build())
                .build()
    }


    static def createOgcApiApiEntity() {
        def entity = new OgcApiApiEntity(registry, null, null, null, null, null)
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