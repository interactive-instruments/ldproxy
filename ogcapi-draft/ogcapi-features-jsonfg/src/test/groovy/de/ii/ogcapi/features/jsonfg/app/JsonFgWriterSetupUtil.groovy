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
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration
import de.ii.ogcapi.features.geojson.domain.ModifiableEncodingAwareContextGeoJson
import de.ii.ogcapi.features.geojson.domain.ModifiableStateGeoJson
import de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration
import de.ii.ogcapi.foundation.app.OgcApiEntity
import de.ii.ogcapi.foundation.domain.ApiMediaType
import de.ii.ogcapi.foundation.domain.ApiRequestContext
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2
import de.ii.ogcapi.foundation.domain.OgcApi
import de.ii.ogcapi.foundation.domain.URICustomizer
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
                .api(new OgcApiEntity(null, null, null, null))
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
                })
                .limit(10)
                .offset(20)
                .maxAllowableOffset(0)
                .isHitsOnly(false)
                .state(ModifiableStateGeoJson.create())
                .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder().enabled(true).useFormattedJsonOutput(true).build())
                .build()

        return ModifiableEncodingAwareContextGeoJson.create().setEncoding(transformationContext)
    }

}
