/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputConformance
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputLandingPage
import de.ii.ldproxy.ogcapi.application.OgcApiDatasetEntity
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.CommonQuery
import de.ii.ldproxy.ogcapi.domain.CommonFormatExtension
import de.ii.ldproxy.ogcapi.domain.ConformanceClass
import de.ii.ldproxy.ogcapi.domain.Dataset
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDatasetData
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext
import de.ii.ldproxy.ogcapi.domain.URICustomizer
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext
import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.EpsgCrs
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer
import spock.lang.Ignore
import spock.lang.PendingFeature
import spock.lang.Specification

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class LandingPageSpec extends Specification {

    static final OgcApiDatasetData datasetData = createDatasetData()
    static OgcApiDatasetEntity ogcApiDatasetEntity = createDatasetEntity()
    static final OgcApiRequestContext requestContext = createRequestContext()
    OgcApiQueriesHandlerCommon queryHandler = new OgcApiQueriesHandlerCommon(createExtensionRegistry())


    @Ignore
    def 'Requirement 1 A: GET support at /'() {
        when: "HTTP GET request at /"
        then: "receive a server response"
    }

    @Ignore
    def 'Requirement 2 B: response code 200'() {
        when: 'HTTP GET request at /'
        then: 'the server responds with HTTP status code 200'
    }

    @PendingFeature
    def 'Requirement 2 B: landing page response'() {

        given: "a request to the landing page"
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build()

        when: "the response is created"
        Dataset landingPage = queryHandler.handle(CommonQuery.DATASET, queryInputDataset, requestContext).entity as Dataset

        then: 'it should comply to landingPage.yml'

        and: 'it should contain a link to the api definition'
        landingPage.links.any { it.rel == 'service-desc' || it.rel == 'service-doc' }

        and: 'it should contain a link to the conformance resource'
        landingPage.links.any { it.rel == 'conformance' && it.href.contains('/conformance') }

        and: 'it should contain a link to the collections resource'
        landingPage.links.any { it.rel == 'data' && it.href.contains('/collections') }

    }

    @Ignore
    def 'Reqiurement 3 A: HTTP GET support for returned URIs'() {
        given: 'list of URIs from the landing page response'
        when: 'an HTTP GET request is made to each URI'
        then: 'a response is received'
    }

    @Ignore
    def 'Requirement 4 A: '() {
        given: 'GET request to the URIs linked from the landing page (link relations service-desc or service-doc)'
        when: 'Accept header has the value of the link property "type"'
        then: 'returned document is consistent with the requested media type'
    }

    @Ignore
    def 'Requirement 5 A: HTTP GET support at /conformance'() {
        when: 'HTTP GET request at /conformance'
        then: 'a response is received'
    }

    @Ignore
    def 'Requirement 6 A: response code 200'() {
        when: 'HTTP GET request at /conformance'
        then: 'the server responds with HTTP status code 200'
    }

    def 'Requirement 6B'() {

        given: 'a request to the conformance page'
        def queryInputConformance = new ImmutableOgcApiQueryInputConformance.Builder().build()

        when: 'the response is created'
        List<ConformanceClass> conformancePage = queryHandler
                .handle(CommonQuery.CONFORMANCE, queryInputConformance, requestContext).entity as List<ConformanceClass>

        then: 'it should return a list of conformance classes that the server conforms to'
        conformancePage.any { it.getConformanceClass() == 'foo bar 1234' }

    }

    @Ignore
    def 'Requirement 7 A: conformance to HTTP 1.1'() {
        expect: 'the server shall conform to HTTP 1.1'
    }

    @Ignore
    def 'Requirement 7 B: conformance to HTTP over TLS'() {
        when: 'the server supports HTTPS'
        then: 'the server shall also conform to HTTP over TLS'
    }

    @PendingFeature
    def 'Requirement 8 A: query parameter not specified in the API definition'() {
        when: 'a request to the landing page with a parameter not specified in the API definition'
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build()
        Dataset landingPage = queryHandler.handle(CommonQuery.DATASET, queryInputDataset,
                createRequestContextUnknownParameter()).entity as Dataset

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalStateException)
    }

    @PendingFeature
    def 'Requirement 9 A: invalid query parameter value'() {
        when: 'a request to the landing page with a URI that has an invalid value'
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build()
        Dataset landingPage = queryHandler.handle(CommonQuery.DATASET, queryInputDataset,
                createRequestContextInvalidParameterValue()).entity as Dataset

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalArgumentException)
    }

    @Ignore
    def 'Requirement 10 A: coordinate reference systems'() {
        expect: 'all spatial geometries shall be in the coordinate reference system'
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
        def entity = new OgcApiDatasetEntity(createExtensionRegistry(), null, null, null, null, null)
        entity.setData(datasetData)
        return entity
    }

    static def createRequestContext() {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .api(ogcApiDatasetEntity)
                .requestUri(new URI('http://example.com'))
                .build()
    }

    static def createRequestContextUnknownParameter() {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build())
                .dataset(datasetData)
                .requestUri(new URI('http://example.com?abc=true'))
                .build()
    }

    static def createRequestContextInvalidParameterValue() {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build())
                .dataset(datasetData)
                .requestUri(new URI('http://example.com?f=abcdf'))
                .build()
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
                if (extensionType == CommonFormatExtension.class) {
                    return ImmutableList.of((T) new CommonFormatExtension() {
                        @Override
                        OgcApiMediaType getMediaType() {
                            return new ImmutableOgcApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        Response getLandingPageResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(dataset).build()
                        }

                        @Override
                        Response getConformanceResponse(List<ConformanceClass> ocgApiConformanceClasses, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok().entity(ocgApiConformanceClasses).build()
                        }
                    })
                }
                if (extensionType == ConformanceClass.class) {
                    return ImmutableList.of((T) new ConformanceClass() {

                        @Override
                        String getConformanceClass() {
                            return 'foo bar 1234'
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDatasetData datasetData) {
                            return true
                        }
                    })
                }

                return ImmutableList.of()
            }
        }
    }

}
