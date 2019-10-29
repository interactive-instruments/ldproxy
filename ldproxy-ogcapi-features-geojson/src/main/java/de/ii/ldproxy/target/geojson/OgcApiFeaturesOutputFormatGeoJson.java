/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.api.TargetMappingRefiner;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
            .build();

    @Requires
    private GeoJsonConfig geoJsonConfig;

    @Requires
    private GeoJsonWriterRegistry geoJsonWriterRegistry;


    @Override
    public String getConformanceClass() {
        return CONFORMANCE_CLASS;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {

        // TODO support language
        ImmutableSortedSet<GeoJsonWriter> geoJsonWriters = geoJsonWriterRegistry.getGeoJsonWriters()
                                                                                .stream()
                                                                                .map(GeoJsonWriter::create)
                                                                                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.comparingInt(GeoJsonWriter::getSortPriority)));

        return Optional.of(new FeatureTransformerGeoJson(ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                                     .from(transformationContext)
                                                                                                     .geoJsonConfig(geoJsonConfig)
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
