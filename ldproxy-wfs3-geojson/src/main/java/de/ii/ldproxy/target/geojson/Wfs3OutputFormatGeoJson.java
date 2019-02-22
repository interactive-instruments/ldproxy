/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3MediaType;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClasses;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatGeoJson implements Wfs3ConformanceClass, Wfs3OutputFormatExtension {

    public static final Wfs3MediaType MEDIA_TYPE = ImmutableWfs3MediaType.builder()
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
        return "http://www.opengis.net/spec/wfs-1/3.0/req/geojson";
    }

    @Override
    public boolean isConformanceEnabledForService(Wfs3ServiceData serviceData) {
        if (isExtensionEnabled(serviceData, GeoJsonConfiguration.class)) {
            return true;
        }
        return false;
    }

    @Override
    public Wfs3MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        if (!isExtensionEnabled(serviceData, GeoJsonConfiguration.class)) {
            return false;
        }
        return true;
    }

    @Override
    public Response getConformanceResponse(List<Wfs3ConformanceClass> wfs3ConformanceClasses, String serviceLabel, Wfs3MediaType wfs3MediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix) {
        return response(new Wfs3ConformanceClasses(wfs3ConformanceClasses.stream()
                                                                         .map(Wfs3ConformanceClass::getConformanceClass)
                                                                         .collect(Collectors.toList())));
    }

    @Override
    public Response getDatasetResponse(Wfs3Collections wfs3Collections, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix, boolean isCollections) {
        return response(wfs3Collections);
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName) {
        return response(wfs3Collection);
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext) {
        ImmutableList<GeoJsonWriter> writers = ImmutableList.of(
                new GeoJsonWriterSkeleton(),
                new GeoJsonWriterId(),
                new GeoJsonWriterMetadata(),
                new GeoJsonWriterGeometry(),
                new GeoJsonWriterProperties(),
                new GeoJsonWriterCrs()
        );

        List<GeoJsonWriter> geoJsonWriters = geoJsonWriterRegistry.getGeoJsonWriters()
                                                                  .stream()
                                                                  .sorted(Comparator.comparingInt(GeoJsonWriter::getSortPriority))
                                                                  .map(GeoJsonWriter::create)
                                                                  .collect(Collectors.toList());

        return Optional.of(new FeatureTransformerGeoJson(ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                                     .from(transformationContext)
                                                                                                     .geoJsonConfig(geoJsonConfig)
                                                                                                     .build(), geoJsonWriters));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2GeoJsonMappingProvider());
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
