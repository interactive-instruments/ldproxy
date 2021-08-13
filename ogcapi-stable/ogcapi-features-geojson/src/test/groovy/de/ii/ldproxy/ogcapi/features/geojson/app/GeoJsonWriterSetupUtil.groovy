/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.geojson.domain.*
import de.ii.xtraplatform.crs.domain.CrsTransformer
import de.ii.xtraplatform.crs.domain.OgcCrs

import javax.ws.rs.core.Request
import java.nio.charset.StandardCharsets

class GeoJsonWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8)
    }

    static EncodingAwareContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection, CrsTransformer crsTransformer = null) throws URISyntaxException {

        FeatureTransformationContextGeoJson transformationContext =  ImmutableFeatureTransformationContextGeoJson.builder()
                .crsTransformer(Optional.ofNullable(crsTransformer))
                .defaultCrs(OgcCrs.CRS84)
                .apiData(new ImmutableOgcApiDataV2.Builder()
                        .id("s")
                        .serviceType("OGC_API")
                /*.featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                        .providerType("WFS")
                        .connectorType("HTTP")
                                                                .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                                              .uri(new URI("http://localhost"))
                                                                                                              .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                              .version("2.0.0")
                                                                                                              .gmlVersion("3.2.1")
                                                                                                              .build())
                                                                .nativeCrs(new EpsgCrs())
                                                                .build())*/
                        .build())
                .collectionId("xyz")
                .outputStream(outputStream)
                .links(ImmutableList.of())
                .isFeatureCollection(isCollection)
                .ogcApiRequest(new ApiRequestContext() {
                    @Override
                    ApiMediaType getMediaType() {
                        return null
                    }

                    @Override
                    List<ApiMediaType> getAlternateMediaTypes() {
                        return null
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
