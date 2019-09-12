/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.application.OgcApiDatasetEntity
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext
import de.ii.ldproxy.wfs3.api.Wfs3CollectionFormatExtension
import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.EpsgCrs
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer
import spock.lang.Ignore
import spock.lang.Specification

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class CollectionsSpec extends Specification {

    static final OgcApiExtensionRegistry registry = createExtensionRegistry()
    static final OgcApiDatasetData datasetData = createDatasetData()
    static final OgcApiDatasetEntity ogcApiDatasetEntity =createDatasetEntity()
    static final OgcApiRequestContext requestContext = createRequestContext()
    static final Wfs3EndpointCore endpoint = new Wfs3EndpointCore(registry)

    @Ignore
    def 'Requirement 11 A: GET support at /collections'() {
        when: 'HTTP GET request at /collections'
        then: 'the server returns a response'
    }

    @Ignore
    def 'Requirement 12 A: '() {
        when: 'HTTP GET request at /collections'
        then: 'the server responds with HTTP status code 200'
    }

    @Ignore
    def 'Requirement 12 B: '() {
        when: 'HTTP GET request at /collections'
        then: 'the content of the response conforms to OpenAPI 3.0 schema'
    }

    def 'Requirement 13 A: collections response'() {
        given: 'A request to the server at /collections'

        when: 'The server returns a response at path /collections'
        Dataset collections = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'the response shall include a link to this document'
        collections.links.any { it.rel == 'self' }

        and: 'a link to the response document in every other media type supported by the server'
        collections.links.any { it.rel == 'alternate' }
    }

    def 'Requirement 14 A: collections response'() {
        given: 'A request to the server at /collections'

        when: 'The server returns a response at path /colections'
        Dataset collections = endpoint.getCollections(Optional.empty(), ogcApiDatasetEntity, requestContext).entity as Dataset

        then: 'each feature shall be provided in the property collections'
        collections.collections.any { it.id == 'id1' }
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
                            datasetBuilder.title('Test title')
                            datasetBuilder.description("Test description")
                            datasetBuilder.addSections(ImmutableMap.of("collections",
                                    ImmutableList.of(new ImmutableOgcApiCollection.Builder()
                                            .id("id1")
                                            .title("title1")
                                            .description("foo bar 1")
                                            .extent(new OgcApiExtent())
                                            .build())))
                            return datasetBuilder
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDatasetData dataset) {
                            return true
                        }
                    })
            }
        }
    }

    static def createRequestContext() {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .alternateMediaTypes(ImmutableList.of(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_XML_TYPE)
                        .build()))
                .api(ogcApiDatasetEntity)
                .requestUri(new URI('http://example.com/collections'))
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
}
