/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ogcapi.features.geojson.domain.*
import de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration
import de.ii.ogcapi.foundation.app.OgcApiEntity
import de.ii.ogcapi.foundation.domain.*
import de.ii.xtraplatform.auth.domain.User
import de.ii.xtraplatform.crs.domain.CrsTransformer
import de.ii.xtraplatform.crs.domain.OgcCrs

import javax.ws.rs.core.Request
import java.nio.charset.StandardCharsets

class JsonFgWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8)
    }

    static EncodingAwareContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection, CrsTransformer crsTransformer = null) throws URISyntaxException {

        FeatureTransformationContextGeoJson transformationContext =  ImmutableFeatureTransformationContextGeoJson.builder()
                .crsTransformer(Optional.ofNullable(crsTransformer))
                .defaultCrs(OgcCrs.CRS84)
                .mediaType(FeaturesFormatJsonFg.MEDIA_TYPE)
                .api(new OgcApiEntity(null, null, null, new AppContextTest(), null, null))
                .apiData(new ImmutableOgcApiDataV2.Builder()
                        .id("s")
                        .serviceType("OGC_API")
                        .addExtensions(new ImmutableJsonFgConfiguration.Builder().enabled(true).coordRefSys(true).build())
                        .build())
                .featureSchemas(ImmutableMap.of("xyz",Optional.empty()))
                .outputStream(outputStream)
                .links(ImmutableList.of())
                .isFeatureCollection(isCollection)
                .ogcApiRequest(new ApiRequestContext() {
                    @Override
                    URI getExternalUri() {
                        return null
                    }

                    @Override
                    ApiMediaType getMediaType() {
                        return FeaturesFormatJsonFg.MEDIA_TYPE

                    }

                    @Override
                    List<ApiMediaType> getAlternateMediaTypes() {
                        return ImmutableList.of()
                    }

                    @Override
                    Optional<Locale> getLanguage() {
                        return Optional.empty()
                    }

                    @Override
                    OgcApi getApi() {
                        return null
                    }

                    @Override
                    URICustomizer getUriCustomizer() {
                        return new URICustomizer()
                    }

                    @Override
                    String getStaticUrlPrefix() {
                        return null
                    }

                    @Override
                    Map<String, String> getParameters() {
                        return null
                    }

                    @Override
                    Optional<Request> getRequest() {
                        return null
                    }

                    @Override
                    Optional<User> getUser() {
                        return null
                    }

                    @Override
                    QueryParameterSet getQueryParameterSet() {
                        return QueryParameterSet.of()
                    }
                })
                .limit(10)
                .offset(20)
                .maxAllowableOffset(0)
                .isHitsOnly(false)
                .state(ModifiableStateGeoJson.create())
                .prettify(true)
                .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder().enabled(true).build())
                .build()

        return ModifiableEncodingAwareContextGeoJson.create().setEncoding(transformationContext)
    }

}
