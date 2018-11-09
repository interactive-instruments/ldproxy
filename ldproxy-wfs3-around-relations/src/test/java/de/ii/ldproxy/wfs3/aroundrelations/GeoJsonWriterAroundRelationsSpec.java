/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.greghaskins.spectrum.Spectrum;
import de.ii.ldproxy.target.geojson.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonConfig;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping;
import de.ii.ldproxy.target.geojson.GeoJsonWriter;
import de.ii.ldproxy.target.geojson.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ldproxy.target.geojson.ModifiableStateGeoJson;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableFeatureProviderDataWfs;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static com.greghaskins.spectrum.dsl.specification.Specification.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterAroundRelationsSpec {

    static final GeoJsonGeometryMapping geometryMapping = new GeoJsonGeometryMapping();

    static final String coordinates = "10 50, 10 51, 11 51, 11 50, 10 50";

    static {
        geometryMapping.setGeometryType(GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POLYGON);
    }

    static String expectedCollection = "{\n" +
            "        \"type\" : \"FeatureCollection\",\n" +
            "        \"links\" : [],\n" +
            "        \"numberMatched\" : 8,\n" +
            "        \"numberReturned\" : 5,\n" +
            "        \"timeStamp\" : \"2018-08-30T15:36:08Z\",\n" +
            "        \"features\" : [ { \"links\" : [ {\"rel\": \"self\", \"href\": \"RELATION\"} ] } ]\n" +
            "      }";

    {

        describe("GeoJsonWriterAroundRelations middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("matching config and query", () -> {

                context("resolve=false", () -> {
                    //runTransformer(outputStream, true, OTHER_CRS);

                    it("it should add links and nothing else", () -> {

                        URI query = new URI("http://test?relations=test1&resolve=false");

                        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, query);

                        final FeatureTransformationContextGeoJson[] nextTransformationContext = new FeatureTransformationContextGeoJson[1];

                        runTransformer(transformationContext, new GeoJsonWriter() {
                            @Override
                            public int getSortPriority() {
                                return 10000;
                            }

                            @Override
                            public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
                                nextTransformationContext[0] = transformationContext;
                            }
                        }, new AroundRelationResolver() {
                            @Override
                            public String resolve(AroundRelationConfiguration.Relation aroundRelationConfiguration, AroundRelationsQuery.AroundRelationQuery aroundRelationQuery) {

                                assertThat(aroundRelationQuery.name).isEqualTo("test1");

                                assertThat(aroundRelationQuery.limit).isEqualTo(5);

                                assertThat(aroundRelationQuery.offset).isEqualTo(0);

                                assertThat(aroundRelationQuery.bbox).isEqualTo("10.000000,50.000000,11.000000,51.000000");


                                return expectedCollection;
                            }
                        });

                        int expectedSize = transformationContext.getLinks()
                                                                .size() + 1;

                        Wfs3Link expectedLink = ImmutableWfs3Link.builder()
                                                                 .rel("self")
                                                                 .href("RELATION")
                                                                 .build();

                        assertThat(nextTransformationContext[0].getLinks()).hasSize(expectedSize)
                                                                           .contains(expectedLink);

                        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                        String expected = "{ }";

                        assertThat(actual).isEqualTo(expected);


                    });

                });

                context("resolve=true", () -> {
                    //runTransformer(outputStream, true, OTHER_CRS);

                    it("it should additionalFeatures and nothing else", () -> {

                        URI query = new URI("http://test?relations=test1&resolve=true");

                        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, query);

                        final FeatureTransformationContextGeoJson[] nextTransformationContext = new FeatureTransformationContextGeoJson[1];

                        runTransformer(transformationContext, new GeoJsonWriter() {
                            @Override
                            public int getSortPriority() {
                                return 10000;
                            }

                            @Override
                            public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
                                nextTransformationContext[0] = transformationContext;
                            }
                        }, (aroundRelationConfiguration, aroundRelationQuery) -> expectedCollection);

                        assertThat(nextTransformationContext[0]).isEqualTo(transformationContext);

                        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                        String expected = "{\n" +
                                "  \"additionalFeatures\" : {\n" +
                                "    \"test1\" : {\n" +
                                "        \"type\" : \"FeatureCollection\",\n" +
                                "        \"links\" : [],\n" +
                                "        \"numberMatched\" : 8,\n" +
                                "        \"numberReturned\" : 5,\n" +
                                "        \"timeStamp\" : \"2018-08-30T15:36:08Z\",\n" +
                                "        \"features\" : [ { \"links\" : [ {\"rel\": \"self\", \"href\": \"RELATION\"} ] } ]\n" +
                                "      }\n" +
                                "  }\n" +
                                "}";

                        assertThat(actual).isEqualTo(expected);


                    });

                });

            });

        });

    }

    private void runTransformer(FeatureTransformationContextGeoJson transformationContext, GeoJsonWriter chainedWriter, AroundRelationResolver aroundRelationResolver) throws IOException, URISyntaxException {


        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(GeoJsonWriterAroundRelations.create(aroundRelationResolver), chainedWriter));

        transformationContext.getJson()
                             .writeStartObject();

        transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        transformer.onFeatureStart(null);
        transformer.onGeometryStart(geometryMapping, SimpleFeatureGeometry.MULTI_POLYGON, null);
        transformer.onGeometryNestedStart();
        transformer.onGeometryCoordinates(coordinates);
        transformer.onGeometryNestedEnd();
        transformer.onGeometryEnd();
        transformer.onFeatureEnd();

        transformationContext.getJson()
                             .writeEndObject();
        transformer.onEnd();
    }

    private FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, URI query) throws URISyntaxException {
        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .crsTransformer(Optional.empty())
                                                           .serviceData(ImmutableWfs3ServiceData.builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
                                                                                                .featureProvider(ImmutableFeatureProviderDataWfs.builder()
                                                                                                                                                .connectionInfo(ImmutableConnectionInfo.builder()
                                                                                                                                                                                       .uri(new URI("http://localhost"))
                                                                                                                                                                                       .method(ConnectionInfo.METHOD.GET)
                                                                                                                                                                                       .version("2.0.0")
                                                                                                                                                                                       .gmlVersion("3.2.1")
                                                                                                                                                                                       .build())
                                                                                                                                                .nativeCrs(new EpsgCrs())
                                                                                                                                                .build())
                                                                                                .featureTypes(ImmutableMap.of("ft", ImmutableFeatureTypeConfigurationWfs3.builder()
                                                                                                                                                                         .id("ft")
                                                                                                                                                                         .label("ft")
                                                                                                                                                                         .extent(new FeatureTypeConfigurationWfs3.FeatureTypeExtent())
                                                                                                                                                                         .putExtensions(AroundRelationConfiguration.EXTENSION_KEY, ImmutableAroundRelationConfiguration.builder()
                                                                                                                                                                                                                                                                       .addRelations(ImmutableRelation.builder()
                                                                                                                                                                                                                                                                                                      .id("test1")
                                                                                                                                                                                                                                                                                                      .label("test1")
                                                                                                                                                                                                                                                                                                      .responseType("application/geo+json")
                                                                                                                                                                                                                                                                                                      .urlTemplate("")
                                                                                                                                                                                                                                                                                                      .build())
                                                                                                                                                                                                                                                                       .build())
                                                                                                                                                                         .build()))
                                                                                                .build())
                                                           .collectionName("ft")
                                                           .outputStream(outputStream)
                                                           .links(ImmutableList.of(ImmutableWfs3Link.builder()
                                                                                                    .href("TEST")
                                                                                                    .build()))
                                                           .isFeatureCollection(false)
                                                           .wfs3Request(new Wfs3RequestContext() {
                                                               @Override
                                                               public Wfs3MediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public URICustomizer getUriCustomizer() {
                                                                   return new URICustomizer(query);
                                                               }

                                                               @Override
                                                               public String getStaticUrlPrefix() {
                                                                   return null;
                                                               }
                                                           })
                                                           .limit(10)
                                                           .offset(20)
                                                           .serviceUrl("")
                                                           .maxAllowableOffset(0)
                                                           .state(ModifiableStateGeoJson.create())
                                                           .geoJsonConfig(new GeoJsonConfig())
                                                           .build();

    }
}