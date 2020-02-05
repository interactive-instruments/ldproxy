/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.domain.OgcCrs;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public class GeoJsonWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection) throws URISyntaxException {
        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .defaultCrs(OgcCrs.CRS84)
                                                           .apiData(new ImmutableOgcApiApiDataV2.Builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
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
                                                           .ogcApiRequest(new OgcApiRequestContext() {
                                                               @Override
                                                               public OgcApiMediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public List<OgcApiMediaType> getAlternateMediaTypes() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public Optional<Locale> getLanguage() {
                                                                   return Optional.empty();
                                                               }

                                                               @Override
                                                               public OgcApiApi getApi() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public URICustomizer getUriCustomizer() {
                                                                   return new URICustomizer();
                                                               }

                                                               @Override
                                                               public String getStaticUrlPrefix() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public Map<String, String> getParameters() {
                                                                   return null;
                                                               }
                                                           })
                                                           .limit(10)
                                                           .offset(20)
                                                           .maxAllowableOffset(0)
                                                           .isHitsOnly(false)
                                                           .state(ModifiableStateGeoJson.create())
                                                           .geoJsonConfig(ImmutableGeoJsonConfig.builder().isEnabled(true).nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST).multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY).useFormattedJsonOutput(true).build())
                                                           .build();

    }
}
