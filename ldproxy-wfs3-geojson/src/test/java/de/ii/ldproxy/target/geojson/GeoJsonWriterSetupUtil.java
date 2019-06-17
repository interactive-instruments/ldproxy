/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableFeatureProviderDataWfs;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * @author zahnen
 */
public class GeoJsonWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection) throws URISyntaxException {
        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .serviceData(ImmutableWfs3ServiceData.builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
                                                                                                .featureProvider(new ImmutableFeatureProviderDataWfs.Builder()
                                                                                                                                                .connectionInfo(ImmutableConnectionInfo.builder()
                                                                                                                                                                                       .uri(new URI("http://localhost"))
                                                                                                                                                                                       .method(ConnectionInfo.METHOD.GET)
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
                                                           .wfs3Request(new Wfs3RequestContext() {
                                                               @Override
                                                               public Wfs3MediaType getMediaType() {
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
                                                           .geoJsonConfig(new GeoJsonConfig())
                                                           .build();

    }
}
