/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.application.*
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.Query
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.core.application.ImmutableOgcApiFeaturesCoreConfiguration
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesLandingPageExtension
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext
import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.EpsgCrs
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer
import spock.lang.PendingFeature
import spock.lang.Specification

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class LandingPageSpec extends Specification {

    static final OgcApiDatasetData datasetData = createDatasetData()
    static OgcApiApiEntity ogcApiApiEntity = createDatasetEntity()
    static final OgcApiRequestContext requestContext = createRequestContext()
    static OgcApiQueriesHandlerCommon queryHandler = new OgcApiQueriesHandlerCommon(createExtensionRegistry())


    def setupSpec() {
        queryHandler.i18n = new I18n()
    }

    def 'Requirement 2 B: landing page response'() {

        given: "a request to the landing page"
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder()
                .includeLinkHeader(false)
                .build()

        when: "the response is created"
        LandingPage landingPage = queryHandler.handle(Query.LANDING_PAGE, queryInputDataset, requestContext).entity as LandingPage

        then: 'it should comply to landingPage.yml'

        and: 'it should contain a link to the api definition'
        landingPage.links.any { it.rel == 'service-desc' || it.rel == 'service-doc' }

        and: 'it should contain a link to the conformance resource'
        landingPage.links.any { it.rel == 'conformance' && it.href.contains('/conformance') }

        and: 'it should contain a link to the collections resource'
        landingPage.links.any { it.rel == 'data' && it.href.contains('/collections') }

    }

    def 'Requirement 6B'() {

        given: 'a request to the conformance page'
        def queryInputConformance = new ImmutableOgcApiQueryInputConformance.Builder()
                .includeLinkHeader(false)
                .includeHomeLink(false)
                .build()

        when: 'the response is created'
        ConformanceDeclaration conformanceDeclaration = queryHandler.handle(Query.CONFORMANCE_DECLARATION,
                queryInputConformance, requestContext).entity as ConformanceDeclaration

        then: 'it should return a list of conformance classes that the server conforms to'
        conformanceDeclaration.conformsTo.any { it == 'foo bar 1234' }

    }

    def 'Requirement 8 A: query parameter not specified in the API definition'() {
        when: 'a request to the landing page with a parameter not specified in the API definition'
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build()
        queryHandler.handle(Query.LANDING_PAGE, queryInputDataset,
                createRequestContext('http://example.com?foo=bar')).entity as LandingPage

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalStateException)
    }

    @PendingFeature
    def 'Requirement 9 A: invalid query parameter value'() {
        when: 'a request to the landing page with a URI that has an invalid parameter value'
        def queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build()
        queryHandler.handle(Query.LANDING_PAGE, queryInputDataset,
                createRequestContext('http://example.com?f=foobar')).entity as LandingPage

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalArgumentException)
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
                .addCapabilities(new ImmutableOgcApiFeaturesCoreConfiguration.Builder().build())
                .build()
    }

    static def createDatasetEntity() {
        def entity = new OgcApiApiEntity(createExtensionRegistry(), null, null, null, null, null)
        entity.setData(datasetData)
        return entity
    }

    static def createRequestContext(String uri = 'http://example.com') {
        new ImmutableOgcApiRequestContext.Builder()
                .mediaType(new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .api(ogcApiApiEntity)
                .requestUri(new URI(uri))
                .build()
    }


    static def createExtensionRegistry() {
        new OgcApiExtensionRegistry() {

            @Override
            List<OgcApiExtension> getExtensions() {
                return ImmutableList.of()
            }

            @Override
            <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
                if (extensionType == OgcApiLandingPageExtension.class) {
                    OgcApiFeaturesLandingPageExtension landingPage = new OgcApiFeaturesLandingPageExtension()
                    landingPage.i18n = new I18n()
                    return ImmutableList.of((T) landingPage)
                }
                if (extensionType == CommonFormatExtension.class) {
                    return ImmutableList.of((T) new CommonFormatExtension() {

                        @Override
                        Response getLandingPageResponse(LandingPage apiLandingPage, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok(apiLandingPage).build()
                        }

                        @Override
                        Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration, OgcApiDataset api, OgcApiRequestContext requestContext) {
                            return Response.ok(conformanceDeclaration).build()
                        }

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
