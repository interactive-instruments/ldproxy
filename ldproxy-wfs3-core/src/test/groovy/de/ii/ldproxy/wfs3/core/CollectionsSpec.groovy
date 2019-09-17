/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core

import ch.qos.logback.classic.Level
import ch.qos.logback.core.Appender
import com.codahale.metrics.MetricRegistry
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.application.OgcApiDatasetEntity
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext
import de.ii.ldproxy.wfs3.api.Wfs3CollectionFormatExtension
import de.ii.ldproxy.wfs3.api.Wfs3FeatureFormatExtension
import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.EpsgCrs
import de.ii.xtraplatform.dropwizard.api.Dropwizard
import de.ii.xtraplatform.dropwizard.api.XtraPlatformConfiguration
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping
import io.dropwizard.jersey.setup.JerseyEnvironment
import io.dropwizard.jetty.MutableServletContextHandler
import io.dropwizard.jetty.setup.ServletEnvironment
import io.dropwizard.setup.Environment
import io.dropwizard.views.ViewRenderer
import org.glassfish.jersey.servlet.ServletContainer
import spock.lang.Ignore
import spock.lang.Specification

import javax.servlet.ServletContext
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.function.Function
import java.util.stream.Collectors

class CollectionsSpec extends Specification {

    static final OgcApiExtensionRegistry registry = createExtensionRegistry()
    static final OgcApiDatasetData datasetData = createDatasetData()
    static final OgcApiDatasetEntity ogcApiDatasetEntity =createDatasetEntity()
    static final OgcApiRequestContext requestContext = createRequestContext()
    static final Wfs3Core wfs3Core = createWfs3Core()
    static final Wfs3EndpointCore endpoint = createWfs3Endpoint()

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
        Dataset collections = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'the response shall include a link to this document'
        collections.links.any { it.rel == 'self' }

        and: 'a link to the response document in every other media type supported by the server'
        collections.links.any { it.rel == 'alternate' }
    }

    def 'Requirement 14 A: collections response'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Dataset result = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'each feature shall be provided in the property collections'
        result.collections.any { it.id == 'featureType1' }
    }

    def 'Requirement 15 A: collections items links'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Dataset result = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'links property of each feature shall include an item for each supported encoding with a link to the features resource'
        def links = result.getCollections().stream().map{x -> x.getLinks()}.collect(Collectors.toList())
        links.any { it.any { it.rel == "items" } }
    }

    def 'Requirement 15 B: collections items links'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Dataset result = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'all links shall include the rel and type properties'
        def links = result.getCollections()
                .stream()
                .map{x -> x.getLinks()}
                .collect(Collectors.toList())
        links.any { it.any { it.rel == "items" && it.type == "text/html"} }
    }

    def 'Requirement 16 A: extent property'() {
        given: 'A request to the server at /collections'

        when: 'The response is created'
        Dataset result = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'the extent property shall provide bounding boxes that include all spatial geometries and time intervals that include all temporal geometries in this collection'
        result.collections.any { it.extent.spatial.bbox[0] == ([-180.0, -90.0, 180.0, 90.0] as double[])}
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
        Dataset allCollections = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset
        def result = endpoint.getCollection(Optional.empty(), "featureType1", ogcApiDatasetEntity, requestContext).entity as OgcApiCollection

        then: 'the values for id, title, description and extent shall be identical to the values in the /collections response'
        allCollections.getCollections().get(0).id == result.id
        allCollections.getCollections().get(0).title == result.title
        allCollections.getCollections().get(0).description == result.description
        allCollections.getCollections().get(0).extent.spatial.bbox == result.extent.spatial.bbox
        allCollections.getCollections().get(0).extent.spatial.crs == result.extent.spatial.crs
        allCollections.getCollections().get(0).extent.temporal.interval == result.extent.temporal.interval
        allCollections.getCollections().get(0).extent.temporal.trs == result.extent.temporal.trs
        println result
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
            void addExtension(OgcApiExtension extension) {

            }

            @Override
            List<OgcApiExtension> getExtensions() {
                return ImmutableList.of()

            }

            @Override
            <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
                if (extensionType == Wfs3CollectionFormatExtension.class) {
                    return ImmutableList.of((T) new Wfs3CollectionFormatExtension() {
                        @Override
                        OgcApiMediaType getMediaType() {
                            return new ImmutableOgcApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        Response getCollectionsResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(dataset).build()
                        }

                        @Override
                        Response getCollectionResponse(OgcApiCollection ogcApiCollection, String collectionName, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(ogcApiCollection).build()
                        }

                        @Override
                        Response getLandingPageResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return null
                        }

                        @Override
                        Response getConformanceResponse(List<ConformanceClass> ocgApiConformanceClasses, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return null
                        }
                    })
                }
                if (extensionType == OgcApiLandingPageExtension.class)
                    return ImmutableList.of((T) new OgcApiLandingPageExtension() {
                        @Override
                        ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData datasetData,
                                                         URICustomizer uriCustomizer, OgcApiMediaType mediaType,
                                                         List<OgcApiMediaType> alternativeMediaTypes) {
                            datasetBuilder.addSections(ImmutableMap.of("collections",
                                    ImmutableList.of(ImmutableOgcApiCollection.builder()
                                            .id("featureType1")
                                            .title("FeatureType 1")
                                            .description("foo bar")
                                            .extent(new OgcApiExtent())
                                            .links(ImmutableList.of(new ImmutableOgcApiLink.Builder()
                                                    .rel("items")
                                                    .type("text/html")
                                                    .href("uri")
                                                    .description("Description1")
                                                    .build(),
                                            new ImmutableOgcApiLink.Builder()
                                                    .rel("items")
                                                    .type("text/html")
                                                    .href("uri")
                                                    .description("Description2")
                                                    .build(),
                                            new ImmutableOgcApiLink.Builder()
                                                    .rel("items")
                                                    .type("text/html")
                                                    .href("uri")
                                                    .description("Description3")
                                                    .build()))
                                            .build())))
                            return datasetBuilder.addSections(ImmutableMap.of())
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDatasetData dataset) {
                            return true
                        }
                    })
                if (extensionType == Wfs3FeatureFormatExtension.class) {
                    return ImmutableList.of((T) new Wfs3FeatureFormatExtension() {

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
                .api(ogcApiDatasetEntity)
                .requestUri(new URI(uri))
                .build()
    }

    static def createDatasetData() {
        new ImmutableOgcApiDatasetData.Builder()
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
                                .putMappings("test", new ImmutableSourcePathMapping.Builder()
                                        .putMappings("general", new Wfs3GenericMapping())
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
                .build()
    }


    static def createDatasetEntity() {
        def entity = new OgcApiDatasetEntity(registry, null, null, null, null, null)
        entity.setData(datasetData)
        return entity
    }

    static def createWfs3Endpoint() {
        def endpoint = new Wfs3EndpointCore(registry)
        endpoint.wfs3Core = wfs3Core
        endpoint.wfs3Query = new Wfs3Query(registry)
        return endpoint
    }

    static Map<OgcApiMediaType, Wfs3FeatureFormatExtension> getFeatureFormats() {
        return registry.getExtensionsForType(Wfs3FeatureFormatExtension.class)
                .stream()
                .map{ outputFormatExtension -> new AbstractMap.SimpleEntry(outputFormatExtension.getMediaType(), outputFormatExtension)}
                .collect(ImmutableMap.toImmutableMap({Map.Entry.&getKey} as Function, {Map.Entry.&getValue} as Function))
    }


    static def createWfs3Core() {
        return new Wfs3Core(registry, new Dropwizard() {
            @Override
            ServletEnvironment getServlets() {
                return null
            }

            @Override
            ServletContext getServletContext() {
                return null
            }

            @Override
            MutableServletContextHandler getApplicationContext() {
                return null
            }

            @Override
            JerseyEnvironment getJersey() {
                return null
            }

            @Override
            ServletContainer getJerseyContainer() {
                return null
            }

            @Override
            void attachLoggerAppender(Appender appender) {

            }

            @Override
            void detachLoggerAppender(Appender appender) {

            }

            @Override
            void setLoggingLevel(Level level) {

            }

            @Override
            Environment getEnvironment() {
                return new Environment(null, null, null, new MetricRegistry(), null)
            }

            @Override
            void resetServer() {

            }

            @Override
            ViewRenderer getMustacheRenderer() {
                return null
            }

            @Override
            XtraPlatformConfiguration getConfiguration() {
                return null
            }

            @Override
            String getUrl() {
                return null
            }
        })
    }

}
