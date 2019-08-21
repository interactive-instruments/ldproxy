/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ConformanceClasses;
import de.ii.ldproxy.ogcapi.domain.Dataset;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OutputFormatExtension;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.TargetMappingRefiner;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
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
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
//TODO
@Provides(specifications = {Wfs3OutputFormatGeoJson.class, ConformanceClass.class, Wfs3OutputFormatExtension.class, OutputFormatExtension.class, OgcApiExtension.class})
@Instantiate
public class Wfs3OutputFormatGeoJson implements ConformanceClass, Wfs3OutputFormatExtension {


    private static final String CONFORMANCE_CLASS = "http://www.opengis.net/spec/wfs-1/3.0/req/geojson";
    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .main(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .metadata(MediaType.APPLICATION_JSON_TYPE)
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
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, GeoJsonConfiguration.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getConformanceResponse(List<ConformanceClass> wfs3ConformanceClasses, String serviceLabel,
                                           OgcApiMediaType ogcApiMediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                           URICustomizer uriCustomizer, String staticUrlPrefix) {
        return response(new ConformanceClasses(wfs3ConformanceClasses.stream()
                                                                     .map(ConformanceClass::getConformanceClass)
                                                                     .collect(Collectors.toList())));
    }

    @Override
    public Response getDatasetResponse(Dataset dataset, OgcApiDatasetData datasetData, OgcApiMediaType mediaType,
                                       List<OgcApiMediaType> alternativeMediaTypes, URICustomizer uriCustomizer,
                                       String staticUrlPrefix, boolean isCollections) {
        return response(dataset);
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, OgcApiDatasetData datasetData,
                                          OgcApiMediaType mediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                          URICustomizer uriCustomizer, String collectionName) {
        return response(wfs3Collection);
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext) {
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
            public SourcePathMapping refine(SourcePathMapping sourcePathMapping, SimpleFeatureGeometry simpleFeatureGeometry) {
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

                        builder.putMappings(entry.getKey(), geoJsonGeometryMapping);
                    } else {
                        builder.putMappings(entry.getKey(), entry.getValue());
                    }
                }

                return builder.build();
            }
        });
    }

    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }
}
