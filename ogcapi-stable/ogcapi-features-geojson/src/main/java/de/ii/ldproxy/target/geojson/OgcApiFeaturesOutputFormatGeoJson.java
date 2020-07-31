/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.api.TargetMappingRefiner;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * @author zahnen
 */
@Component
//TODO
@Provides(specifications = {OgcApiFeaturesOutputFormatGeoJson.class, ConformanceClass.class, OgcApiFeatureFormatExtension.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OgcApiFeaturesOutputFormatGeoJson implements ConformanceClass, OgcApiFeatureFormatExtension {

    private static final String CONFORMANCE_CLASS = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .parameter("json")
            .build();
    public static final OgcApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Requires
    SchemaGeneratorFeature schemaGeneratorFeature;

    @Requires
    SchemaGeneratorFeatureCollection schemaGeneratorFeatureCollection;

    @Requires
    GeoJsonWriterRegistry geoJsonWriterRegistry;

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of(CONFORMANCE_CLASS);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), GeoJsonConfiguration.class);
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        String schemaRef = "#/components/schemas/anyObject";
        Schema schema = new ObjectSchema();
        String collectionId = path.split("/", 4)[2];
        if (path.matches("/collections/[^//]+/items/?")) {
            schemaRef = schemaGeneratorFeatureCollection.getSchemaReferenceOpenApi(collectionId);
            schema = schemaGeneratorFeatureCollection.getSchemaOpenApi(apiData, collectionId);
        } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
            schemaRef = schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId);
            schema = schemaGeneratorFeature.getSchemaOpenApi(apiData, collectionId);
        }
        // TODO example
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public OgcApiMediaTypeContent getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
        String collectionId = path.split("/", 4)[2];
        if ((path.matches("/collections/[^//]+/items/[^//]+/?") && method== OgcApiContext.HttpMethods.PUT) ||
            (path.matches("/collections/[^//]+/items/?") && method== OgcApiContext.HttpMethods.POST)) {
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaGeneratorFeature.getSchemaOpenApi(apiData, collectionId))
                    .schemaRef(schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId))
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();
        }

        return null;
    }

    @Override
    public OgcApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext,
                                                               Optional<Locale> language) {

        // TODO support language
        ImmutableSortedSet<GeoJsonWriter> geoJsonWriters = geoJsonWriterRegistry.getGeoJsonWriters()
                                                                                .stream()
                                                                                .map(GeoJsonWriter::create)
                                                                                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.comparingInt(GeoJsonWriter::getSortPriority)));

        return Optional.of(new FeatureTransformerGeoJson(ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                                     .from(transformationContext)
                                                                                                     .geoJsonConfig(transformationContext.getApiData().getCollections().get(transformationContext.getCollectionId()).getExtension(GeoJsonConfiguration.class).get())
                                                                                                     .prettify(Optional.ofNullable(transformationContext.getOgcApiRequest()
                                                                                                                                                        .getParameters()
                                                                                                                                                        .get("pretty"))
                                                                                                                       .filter(value -> Objects.equals(value, "true"))
                                                                                                                       .isPresent())
                                                                                                     .debugJson(Optional.ofNullable(transformationContext.getOgcApiRequest()
                                                                                                                                                        .getParameters()
                                                                                                                                                        .get("debug"))
                                                                                                                       .filter(value -> Objects.equals(value, "true"))
                                                                                                                       .isPresent())
                                                                                                     .build(), geoJsonWriters));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2GeoJsonMappingProvider());
    }

    @Override
    public Optional<TargetMappingRefiner> getMappingRefiner() {
        return Optional.of(new TargetMappingRefiner() {
            @Override
            public boolean needsRefinement(SourcePathMapping sourcePathMapping) {
                if (!sourcePathMapping.hasMappingForType(Gml2GeoJsonMappingProvider.MIME_TYPE)
                        || !(sourcePathMapping.getMappingForType(Gml2GeoJsonMappingProvider.MIME_TYPE) instanceof GeoJsonGeometryMapping)) {
                    return false;
                }
                GeoJsonGeometryMapping geoJsonGeometryMapping = (GeoJsonGeometryMapping) sourcePathMapping.getMappingForType(Gml2GeoJsonMappingProvider.MIME_TYPE);

                return geoJsonGeometryMapping.getGeometryType() == GEO_JSON_GEOMETRY_TYPE.GENERIC;
            }

            @Override
            public SourcePathMapping refine(SourcePathMapping sourcePathMapping,
                                            SimpleFeatureGeometry simpleFeatureGeometry, boolean mustReversePolygon) {
                if (!needsRefinement(sourcePathMapping)) {
                    return sourcePathMapping;
                }

                ImmutableSourcePathMapping.Builder builder = new ImmutableSourcePathMapping.Builder();

                for (Map.Entry<String, TargetMapping> entry : sourcePathMapping.getMappings()
                                                                               .entrySet()) {
                    TargetMapping targetMapping = entry.getValue();

                    if (targetMapping instanceof GeoJsonGeometryMapping) {
                        GeoJsonGeometryMapping geoJsonGeometryMapping = new GeoJsonGeometryMapping((GeoJsonGeometryMapping) targetMapping);
                        geoJsonGeometryMapping.setGeometryType(GEO_JSON_GEOMETRY_TYPE.forGmlType(simpleFeatureGeometry));

                        if (mustReversePolygon && (simpleFeatureGeometry == SimpleFeatureGeometry.POLYGON || simpleFeatureGeometry == SimpleFeatureGeometry.MULTI_POLYGON)) {
                            geoJsonGeometryMapping.setMustReversePolygon(true);
                        }

                        builder.putMappings(entry.getKey(), geoJsonGeometryMapping);
                    } else {
                        builder.putMappings(entry.getKey(), entry.getValue());
                    }
                }

                return builder.build();
            }
        });
    }
}
