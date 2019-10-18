/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.persistence;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.event.store.EntityHydrator;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderRegistry;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

@Component
@Provides(properties = {
        //TODO: how to connect to entity
        @StaticServiceProperty(name = "entityType", type = "java.lang.String", value = "services")
})
@Instantiate
public class OgcApiDatasetHydrator implements EntityHydrator<OgcApiDatasetData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetHydrator.class);

    @Requires
    private FeatureProviderRegistry featureProviderFactory;

    @Requires
    private CrsTransformation crsTransformerFactory;


    @Override
    public Map<String, Object> getInstanceConfiguration(OgcApiDatasetData data) {
        try {
            TransformingFeatureProvider featureProvider = (TransformingFeatureProvider) featureProviderFactory.createFeatureProvider(data.getFeatureProvider());

            try {
                EpsgCrs sourceCrs = data.getFeatureProvider()
                                        .getNativeCrs();
                CrsTransformer defaultTransformer = crsTransformerFactory.getTransformer(sourceCrs, OgcApiDatasetData.DEFAULT_CRS);
                CrsTransformer defaultReverseTransformer = crsTransformerFactory.getTransformer(OgcApiDatasetData.DEFAULT_CRS, sourceCrs);
                Map<String, CrsTransformer> additionalTransformers = new HashMap<>();
                Map<String, CrsTransformer> additionalReverseTransformers = new HashMap<>();

                data.getAdditionalCrs()
                    .forEach(crs -> {
                        additionalTransformers.put(crs.getAsUri(), crsTransformerFactory.getTransformer(sourceCrs, crs));
                        additionalReverseTransformers.put(crs.getAsUri(), crsTransformerFactory.getTransformer(crs, sourceCrs));
                    });

                OgcApiDatasetData newData = data;
                if (hasMissingBboxes(data.getFeatureTypes())) {
                    ImmutableMap<String, FeatureTypeConfigurationOgcApi> featureTypesWithComputedBboxes = computeMissingBboxes(data.getFeatureTypes(), featureProvider, defaultTransformer);

                    newData = new ImmutableOgcApiDatasetData.Builder()
                            .from(data)
                            .featureTypes(featureTypesWithComputedBboxes)
                            .build();
                }

                LOGGER.debug("TRANSFORMER {} {} -> {} {}", sourceCrs.getCode(), sourceCrs.isForceLongitudeFirst() ? "lonlat" : "latlon", OgcApiDatasetData.DEFAULT_CRS.getCode(), OgcApiDatasetData.DEFAULT_CRS.isForceLongitudeFirst() ? "lonlat" : "latlon");

                return ImmutableMap.<String, Object>builder()
                        .put("data", newData)
                        .put("featureProvider", featureProvider)
                        .put("defaultTransformer", defaultTransformer)
                        .put("defaultReverseTransformer", defaultReverseTransformer)
                        .put("additionalTransformers", additionalTransformers)
                        .put("additionalReverseTransformers", additionalReverseTransformers)
                        .build();

            } catch (Throwable e) {
                LOGGER.error("CRS transformer could not be created"/*, e*/);
                throw e;
            }


        } catch (IllegalStateException e) {
            LOGGER.error("Service with id '{}' could not be created: {}", data.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception:", e);
            }
        }

        throw new IllegalStateException("xyz");
    }

    private boolean hasMissingBboxes(Map<String, FeatureTypeConfigurationOgcApi> featureTypes) {
        return featureTypes
                .entrySet()
                .stream()
                .anyMatch(entry -> Objects.isNull(entry.getValue()
                                                       .getExtent()
                                                       .getSpatial()));
    }

    //TODO Test
    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingBboxes(
            Map<String, FeatureTypeConfigurationOgcApi> featureTypes, FeatureProvider featureProvider,
            CrsTransformer defaultTransformer) throws IllegalStateException {
        return featureTypes
                .entrySet()
                .stream()
                .map(entry -> {

                    if (Objects.isNull(entry.getValue()
                                            .getExtent()
                                            .getSpatial())) {
                        boolean isComputed = true;
                        BoundingBox bbox = null;
                        try {
                            bbox = defaultTransformer.transformBoundingBox(featureProvider.getSpatialExtent(entry.getValue()
                                                                                                                 .getId()));
                        } catch (CrsTransformationException | CompletionException e) {
                            bbox = new BoundingBox(-180.0, -90.0, 180.0, 90.0, new EpsgCrs(4326, true));
                        }

                        ImmutableFeatureTypeConfigurationOgcApi featureTypeConfigurationWfs3 = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                .from(entry.getValue())
                                .extent(new ImmutableCollectionExtent.Builder()
                                        .from(entry.getValue()
                                                   .getExtent())
                                        .spatial(bbox)
                                        .spatialComputed(isComputed)
                                        .build())
                                .build();


                        return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfigurationWfs3);
                    }
                    return entry;
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
