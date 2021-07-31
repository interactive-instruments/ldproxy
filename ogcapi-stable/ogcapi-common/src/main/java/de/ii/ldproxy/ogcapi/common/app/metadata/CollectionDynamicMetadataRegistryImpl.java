/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.metadata;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataCount;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentSpatial;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentTemporal;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataLastModified;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataEntry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Provides
@Instantiate
public class CollectionDynamicMetadataRegistryImpl implements CollectionDynamicMetadataRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionDynamicMetadataRegistryImpl.class);

    private final Map<String, Map<String, Map<MetadataType, CollectionMetadataEntry>>> metadataMap;
    private final CrsTransformerFactory crsTransformerFactory;

    public CollectionDynamicMetadataRegistryImpl(@Requires CrsTransformerFactory crsTransformerFactory) {
        this.crsTransformerFactory = crsTransformerFactory;
        this.metadataMap = new ConcurrentHashMap<>();
    }

    @Override
    public void put(String apiId, String collectionId, MetadataType type, CollectionMetadataEntry value) {
        Map<MetadataType, CollectionMetadataEntry> typeMap = getTypeMap(apiId, collectionId);
        typeMap.put(type, value);
        LOGGER.debug("Metadata '{}' of collection '{}' in API '{}' added as {}.", type, collectionId, apiId, value.getValue().toString());
    }

    @Override
    public boolean update(String apiId, String collectionId, MetadataType type, CollectionMetadataEntry delta) {
        Map<MetadataType, CollectionMetadataEntry> typeMap = getTypeMap(apiId, collectionId);
        CollectionMetadataEntry current = typeMap.get(type);
        if (Objects.isNull(current)) {
            typeMap.put(type, delta);
            LOGGER.debug("Metadata '{}' of collection '{}' in API '{}' changed to {}.", type, collectionId, apiId, delta.getValue().toString());
            return false;
        }

        Optional<CollectionMetadataEntry> updated = current.updateWith(delta);
        updated.ifPresentOrElse(value -> {
            typeMap.put(type, value);
            LOGGER.debug("Metadata '{}' of collection '{}' in API '{}' updated to {}.", type, collectionId, apiId, value.getValue().toString());
        }, () -> LOGGER.trace("Metadata '{}' of collection '{}' in API '{}' unchanged as {}", type, collectionId, apiId, current.getValue().toString()));
        return updated.isPresent();
    }

    @Override
    public boolean remove(String apiId, String collectionId, MetadataType type) {
        Map<MetadataType, CollectionMetadataEntry> typeMap = getTypeMap(apiId, collectionId);
        LOGGER.debug("Metadata '{}' of collection '{}' in API '{}' removed.", type, collectionId, apiId);
        return Objects.nonNull(typeMap.remove(type));
    }

    @Override
    public Optional<CollectionMetadataEntry> get(String apiId, String collectionId, MetadataType type) {
        Map<MetadataType, CollectionMetadataEntry> typeMap = getTypeMap(apiId, collectionId);
        return Optional.ofNullable(typeMap.get(type));
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String apiId) {
        return getApiMap(apiId).entrySet()
                               .stream()
                               .map(entry -> entry.getValue().get(MetadataType.spatialExtent))
                               .filter(Objects::nonNull)
                               .map(CollectionMetadataExtentSpatial.class::cast)
                               .map(CollectionMetadataExtentSpatial::getValue)
                               .map(BoundingBox::toArray)
                               .reduce((doubles, doubles2) -> new double[]{
                                       Math.min(doubles[0], doubles2[0]),
                                       Math.min(doubles[1], doubles2[1]),
                                       Math.max(doubles[2], doubles2[2]),
                                       Math.max(doubles[3], doubles2[3])})
                               .map(doubles -> BoundingBox.of(doubles[0], doubles[1], doubles[2], doubles[3], OgcCrs.CRS84));
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String apiId, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent(apiId);

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), targetCrs));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String apiId, String collectionId) {
        return Optional.ofNullable(getTypeMap(apiId, collectionId).get(MetadataType.spatialExtent))
                       .map(CollectionMetadataExtentSpatial.class::cast)
                       .map(CollectionMetadataExtentSpatial::getValue);
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String apiId, String collectionId, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent(apiId, collectionId);

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), targetCrs));
        }

        return Optional.empty();
    }

    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get()
                                 .transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }

    @Override
    public Optional<TemporalExtent> getTemporalExtent(String apiId) {
        return getApiMap(apiId).entrySet()
                               .stream()
                               .map(entry -> entry.getValue().get(MetadataType.temporalExtent))
                               .filter(Objects::nonNull)
                               .map(CollectionMetadataExtentTemporal.class::cast)
                               .map(CollectionMetadataExtentTemporal::getValue)
                               .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()})
                               .reduce((longs, longs2) -> new Long[]{
                                       longs[0] == null || longs2[0] == null ? null : Math.min(longs[0], longs2[0]),
                                       longs[1] == null || longs2[1] == null ? null : Math.max(longs[1], longs2[1])})
                               .map(longs -> new ImmutableTemporalExtent.Builder().start(longs[0])
                                                                                  .end(longs[1])
                                                                                  .build());
    }

    @Override
    public Optional<TemporalExtent> getTemporalExtent(String apiId, String collectionId) {
        return Optional.ofNullable(getTypeMap(apiId, collectionId).get(MetadataType.temporalExtent))
                       .map(CollectionMetadataExtentTemporal.class::cast)
                       .map(CollectionMetadataExtentTemporal::getValue);
    }

    @Override
    public Optional<Instant> getLastModified(String apiId) {
        return getApiMap(apiId).entrySet()
                               .stream()
                               .map(entry -> entry.getValue().get(MetadataType.lastModified))
                               .filter(Objects::nonNull)
                               .map(CollectionMetadataLastModified.class::cast)
                               .map(CollectionMetadataLastModified::getValue)
                               .max(Instant::compareTo);
    }

    @Override
    public Optional<Instant> getLastModified(String apiId, String collectionId) {
        return Optional.ofNullable(getTypeMap(apiId, collectionId).get(MetadataType.lastModified))
                       .map(CollectionMetadataLastModified.class::cast)
                       .map(CollectionMetadataLastModified::getValue);
    }

    @Override
    public Optional<Long> getItemCount(String apiId) {
        return getApiMap(apiId).entrySet()
                               .stream()
                               .map(entry -> entry.getValue().get(MetadataType.count))
                               .filter(Objects::nonNull)
                               .map(CollectionMetadataCount.class::cast)
                               .map(CollectionMetadataCount::getValue)
                               .reduce(Long::sum);
    }

    @Override
    public Optional<Long> getItemCount(String apiId, String collectionId) {
        return Optional.ofNullable(getTypeMap(apiId, collectionId).get(MetadataType.count))
                       .map(CollectionMetadataCount.class::cast)
                       .map(CollectionMetadataCount::getValue);
    }

    private Map<MetadataType, CollectionMetadataEntry> getTypeMap(String apiId, String collectionId) {
        return metadataMap.computeIfAbsent(apiId, ignore -> new ConcurrentHashMap<>())
                          .computeIfAbsent(collectionId, ignore -> new ConcurrentHashMap<>());

    }

    private Map<String, Map<MetadataType, CollectionMetadataEntry>> getApiMap(String apiId) {
        return metadataMap.computeIfAbsent(apiId, ignore -> new ConcurrentHashMap<>());

    }
}
