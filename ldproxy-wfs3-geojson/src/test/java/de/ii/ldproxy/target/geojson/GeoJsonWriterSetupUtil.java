/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author zahnen
 */
public class GeoJsonWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection) throws URISyntaxException {
        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .serviceData(new ImmutableOgcApiDatasetData.Builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
                                                                                                .featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                                                                                                        .providerType("WFS")
                                                                                                        .connectorType("HTTP")
                                                                                                                                                .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                                                                                                                              .uri(new URI("http://localhost"))
                                                                                                                                                                                              .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                                                                                                              .version("2.0.0")
                                                                                                                                                                                              .gmlVersion("3.2.1")
                                                                                                                                                                                              .build())
                                                                                                                                                .nativeCrs(new EpsgCrs())
                                                                                                                                                .build())
                                                                                                .build())
                                                           .collectionName("xyz")
                                                           .outputStream(outputStream)
                                                           .links(ImmutableList.of())
                                                           .isFeatureCollection(isCollection)
                                                           .wfs3Request(new OgcApiRequestContext() {
                                                               @Override
                                                               public OgcApiMediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public List<OgcApiMediaType> getAlternativeMediaTypes() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public OgcApiDatasetData getDataset() {
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
