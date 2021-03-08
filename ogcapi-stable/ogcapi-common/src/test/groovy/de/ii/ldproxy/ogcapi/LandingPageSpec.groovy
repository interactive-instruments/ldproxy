/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.app.I18nDefault
import de.ii.ldproxy.ogcapi.app.OgcApiEntity
import de.ii.ldproxy.ogcapi.common.app.ImmutableQueryInputConformance
import de.ii.ldproxy.ogcapi.common.app.ImmutableQueryInputLandingPage
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommon
import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration
import de.ii.ldproxy.ogcapi.common.domain.ImmutableCommonConfiguration
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage
import de.ii.ldproxy.ogcapi.common.domain.LandingPage
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension
import de.ii.ldproxy.ogcapi.domain.ApiExtension
import de.ii.ldproxy.ogcapi.domain.ApiMediaType
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext
import de.ii.ldproxy.ogcapi.domain.ConformanceClass
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2
import de.ii.ldproxy.ogcapi.domain.ImmutableRequestContext
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent
import de.ii.ldproxy.ogcapi.domain.OgcApi
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2
import de.ii.ldproxy.ogcapi.domain.URICustomizer
import de.ii.xtraplatform.crs.domain.BoundingBox
import de.ii.xtraplatform.crs.domain.OgcCrs
import io.swagger.v3.oas.models.media.ObjectSchema
import spock.lang.PendingFeature
import spock.lang.Specification

import javax.ws.rs.core.MediaType

class LandingPageSpec extends Specification {

    static final OgcApiDataV2 datasetData = createDatasetData()
    static OgcApiEntity apiEntity = createDatasetEntity()
    static final ApiRequestContext requestContext = createRequestContext()
    static QueriesHandlerCommon queryHandler = new QueriesHandlerCommon(createExtensionRegistry())


    def setupSpec() {
        queryHandler.i18n = new I18nDefault()
    }

    def 'Requirement 2 B: landing page response'() {

        given: "a request to the landing page"
        def queryInputDataset = new ImmutableQueryInputLandingPage.Builder()
                .includeLinkHeader(false)
                .build()

        when: "the response is created"
        LandingPage landingPage = queryHandler.handle(QueriesHandlerCommon.Query.LANDING_PAGE, queryInputDataset, requestContext).entity as LandingPage

        then: 'it should comply to landingPage.yml'

        /* TODO move to OAS30 module
        and: 'it should contain a link to the api definition'
        landingPage.links.any { it.rel == 'service-desc' }

        and: 'it should contain a link to the api documentation'
        landingPage.links.any { it.rel == 'service-doc' }
         */

        and: 'it should contain a link to the conformance resource'
        landingPage.links.any { it.rel == 'conformance' && it.href.contains('/conformance') }

        /* TODO move to COLLECTIONS module
        and: 'it should contain a link to the collections resource'
        landingPage.links.any { it.rel == 'data' && it.href.contains('/collections') }
         */

    }

    def 'Requirement 6B'() {

        given: 'a request to the conformance page'
        def queryInputConformance = new ImmutableQueryInputConformance.Builder()
                .includeLinkHeader(false)
                .build()

        when: 'the response is created'
        ConformanceDeclaration conformanceDeclaration = queryHandler.handle(QueriesHandlerCommon.Query.CONFORMANCE_DECLARATION,
                queryInputConformance, requestContext).entity as ConformanceDeclaration

        then: 'it should return a list of conformance classes that the server conforms to'
        conformanceDeclaration.conformsTo.any { it == 'http://www.opengis.net/spec/ogcapi-common-1/0.0/conf/core' }

    }

    def 'Requirement 8 A: query parameter not specified in the API definition'() {
        when: 'a request to the landing page with a parameter not specified in the API definition'
        def queryInputDataset = new ImmutableQueryInputLandingPage.Builder().build()
        queryHandler.handle(QueriesHandlerCommon.Query.LANDING_PAGE, queryInputDataset,
                createRequestContext('http://example.com?foo=bar')).entity as LandingPage

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalStateException)
    }

    def 'Requirement 9 A: invalid query parameter value'() {
        when: 'a request to the landing page with a URI that has an invalid parameter value'
        def queryInputDataset = new ImmutableQueryInputLandingPage.Builder().build()
        queryHandler.handle(QueriesHandlerCommon.Query.LANDING_PAGE, queryInputDataset,
                createRequestContext('http://example.com?f=foobar')).entity as LandingPage

        then: 'an exception is thrown resulting in a response with HTTP status code 400'
        thrown(IllegalStateException)
    }

    static def createDatasetData() {
        new ImmutableOgcApiDataV2.Builder()
                .id('test')
                .serviceType('OGC_API')
                .putCollections('featureType1', new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                        .id('featureType1')
                        .label('FeatureType 1')
                        .description('foo bar')
                        .extent(new ImmutableCollectionExtent.Builder()
                                .spatial(BoundingBox.of(-180.0, -90.0, 180.0, 90.0, OgcCrs.CRS84))
                                .temporal(new ImmutableTemporalExtent.Builder().build())
                                .build())
                        .build())
                .addExtensions(new ImmutableCommonConfiguration.Builder().build())
                .build()
    }

    static def createDatasetEntity() {
        def entity = new OgcApiEntity(createExtensionRegistry())
        entity.setData(datasetData)
        return entity
    }

    static def createRequestContext(String uri = 'http://example.com') {
        new ImmutableRequestContext.Builder()
                .mediaType(new ImmutableApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build())
                .api(apiEntity)
                .requestUri(new URI(uri))
                .build()
    }


    static def createExtensionRegistry() {
        new ExtensionRegistry() {

            @Override
            List<ApiExtension> getExtensions() {
                return ImmutableList.of()
            }

            @Override
            <T extends ApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
                if (extensionType == LandingPageExtension.class) {
                    LandingPageExtension landingPage = new LandingPageExtension() {
                        @Override
                        ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData, URICustomizer uriCustomizer, ApiMediaType mediaType, List<ApiMediaType> alternateMediaTypes, Optional<Locale> language) {
                            return landingPageBuilder
                        }
                    }
                    return ImmutableList.of((T) landingPage)
                }
                if (extensionType == CommonFormatExtension.class) {
                    return ImmutableList.of((T) new CommonFormatExtension() {

                        @Override
                        ApiMediaType getMediaType() {
                            return new ImmutableApiMediaType.Builder()
                                    .type(MediaType.APPLICATION_JSON_TYPE)
                                    .build()
                        }

                        @Override
                        ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
                            return new ImmutableApiMediaTypeContent.Builder()
                            .ogcApiMediaType(getMediaType())
                            .schema(new ObjectSchema())
                            .schemaRef("#/")
                            .build()
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDataV2 apiData) {
                            return true
                        }

                        @Override
                        Object getLandingPageEntity(LandingPage apiLandingPage, OgcApi api, ApiRequestContext requestContext) {
                            return apiLandingPage
                        }

                        @Override
                        Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration, OgcApi api, ApiRequestContext requestContext) {
                            return conformanceDeclaration
                        }
                    })
                }

                if (extensionType == ConformanceClass.class) {
                    return ImmutableList.of((T) new ConformanceClass() {

                        @Override
                        List<String> getConformanceClassUris() {
                            return ImmutableList.of('http://www.opengis.net/spec/ogcapi-common-1/0.0/conf/core')
                        }

                        @Override
                        boolean isEnabledForApi(OgcApiDataV2 datasetData) {
                            return true
                        }
                    })
                }

                return ImmutableList.of()
            }
        }
    }

}
