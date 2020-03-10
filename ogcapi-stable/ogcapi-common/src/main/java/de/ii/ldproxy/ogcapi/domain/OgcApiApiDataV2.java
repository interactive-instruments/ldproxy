/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiApiDataV2.Builder.class)
public abstract class OgcApiApiDataV2 implements ServiceData, ExtendableConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiDataV2.class);

    static abstract class Builder implements EntityDataBuilder<OgcApiApiDataV2> {
    }

    @Value.Default
    @Override
    public long getEntitySchemaVersion() {
        return 2;
    }

    @Value.Default
    @Override
    public String getServiceType() {
        return "OGC_API";
    }

    public abstract Optional<Metadata> getMetadata();

    @JsonProperty(value = "api")
    @Override
    public abstract List<ExtensionConfiguration> getExtensions();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    //@JsonMerge
    public abstract ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getCollections();

    @Override
    @Value.Derived
    public boolean isLoading() {
        //TODO: delegate to extensions?
        return false;
        //return Objects.nonNull(getFeatureProvider().getMappingStatus()) && getFeatureProvider().getMappingStatus()
        //                           .getLoading();
    }

    @Override
    @Value.Derived
    public boolean hasError() {
        //TODO: delegate to extensions?
        return false;
        /*return Objects.nonNull(getFeatureProvider().getMappingStatus())
                && getFeatureProvider().getMappingStatus().getEnabled()
                && !getFeatureProvider().getMappingStatus().getSupported()
                && Objects.nonNull(getFeatureProvider().getMappingStatus().getErrorMessage());*/
    }

    public boolean isCollectionEnabled(final String collectionId) {
        return getCollections().containsKey(collectionId) && getCollections().get(collectionId).getEnabled();
    }

    /**
     * Determine spatial extent of all collections in the dataset.
     * @return the bounding box in the default CRS
     */
    @Nullable
    @JsonIgnore
    @Value.Derived
    public BoundingBox getSpatialExtent() {
        double[] val = getCollections().values()
                                       .stream()
                                       .map(FeatureTypeConfigurationOgcApi::getExtent)
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getSpatial)
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .map(BoundingBox::getCoords)
                                       .reduce((doubles, doubles2) -> new double[]{
                                                Math.min(doubles[0], doubles2[0]),
                                                Math.min(doubles[1], doubles2[1]),
                                                Math.max(doubles[2], doubles2[2]),
                                                Math.max(doubles[3], doubles2[3])})
                                       .orElse(null);

        return Objects.nonNull(val) ? new BoundingBox(val[0], val[1], val[2], val[3], OgcCrs.CRS84) : null;
    }

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box
     */
    public BoundingBox getSpatialExtent(CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent();

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    public BoundingBox getSpatialExtent(String collectionId) {
        return getCollections().values()
                               .stream()
                               .filter(featureTypeConfiguration -> featureTypeConfiguration.getId().equals(collectionId))
                               .map(FeatureTypeConfigurationOgcApi::getExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getSpatial)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .findFirst()
                               .orElse(null);
    }

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     * @param collectionId the name of the feature type
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box in the target CRS
     */
    public BoundingBox getSpatialExtent(String collectionId, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent(collectionId);

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get().transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }

    // TODO: Or should this be moved elsewhere?
    public SchemaObject getSchema(FeatureType featureType) {
        SchemaObject featureTypeObject = new SchemaObject();

        String collectionId = featureType.getName();
        FeatureTypeConfigurationOgcApi featureTypeApi = getCollections().get(collectionId);
        if (Objects.nonNull(featureType) && Objects.nonNull(featureTypeApi)) {
            ArrayList<FeatureProperty> featureProperties = new ArrayList<>(featureType.getProperties().values());
            featureTypeObject.id = collectionId;
            featureTypeObject.title = Optional.of(featureTypeApi.getLabel());
            featureTypeObject.description = featureTypeApi.getDescription();
            AtomicInteger typeIdx = new AtomicInteger(1);

            featureType.getProperties()
                    .forEach((name, featureProperty) -> {
                        // remove content in square brackets, just in case
                        String nameNormalized = name.replaceAll("\\[[^\\]]+\\]", "[]");

                        String baseName = featureProperty.getName();
                        List<String> baseNameSections = Splitter.on('.').splitToList(baseName);
                        Map<String, String> addInfo = featureProperty.getAdditionalInfo();
                        boolean link = false;
                        List<String> htmlNameSections = ImmutableList.of();
                        String geometryType = null;
                        if (Objects.nonNull(addInfo)) {
                            if (addInfo.containsKey("role")) {
                                link = addInfo.get("role").startsWith("LINK");
                            }
                            if (addInfo.containsKey("title")) {
                                htmlNameSections = Splitter.on('|').splitToList(addInfo.get("title"));
                            }
                            if (addInfo.containsKey("geometryType")) {
                                geometryType = addInfo.get("geometryType");
                            }
                        }

                        // determine context in the properties of this feature
                        String curPath = null;
                        SchemaObject valueContext = featureTypeObject;
                        int arrays = 0;
                        int objectLevel = 0;
                        for (String nameComponent : baseNameSections) {
                            curPath = Objects.isNull(curPath) ? nameComponent : curPath.concat("."+nameComponent);
                            if (link && curPath.equals(baseName)) {
                                // already processed
                                continue;
                            }
                            boolean isArray = nameComponent.endsWith("]");
                            SchemaProperty property = valueContext.get(curPath);
                            if (Objects.isNull(property)) {
                                property = new SchemaProperty();
                                property.id = nameComponent.replace("[]", "");
                                property.maxItems = isArray ? Integer.MAX_VALUE : 1;
                                property.path = curPath;
                                valueContext.properties.add(property);
                            }
                            if (curPath.equals(baseName) ||
                                    link && (curPath.concat(".href").equals(baseName) || curPath.concat(".title").equals(baseName))) {
                                // we are at the end of the path;
                                // this includes the special case of a link object that is mapped to a single value in the HTML
                                if (!htmlNameSections.isEmpty())
                                    property.title = Optional.ofNullable(htmlNameSections.get(Math.min(objectLevel,htmlNameSections.size()-1)));
                                if (link) {
                                    property.wellknownType = Optional.of("Link");
                                } else {
                                    switch (featureProperty.getType()) {
                                        case GEOMETRY:
                                            property.wellknownType = Objects.nonNull(geometryType) ? Optional.of(geometryType) : Optional.of("Geometry");
                                            break;
                                        case INTEGER:
                                            property.literalType = Optional.of("integer");
                                            break;
                                        case FLOAT:
                                            property.literalType = Optional.of("number");
                                            break;
                                        case STRING:
                                            property.literalType = Optional.of("string");
                                            break;
                                        case BOOLEAN:
                                            property.literalType = Optional.of("boolean");
                                            break;
                                        case DATETIME:
                                            property.literalType = Optional.of("dateTime");
                                            break;
                                        default:
                                            return;
                                    }
                                }
                            } else {
                                // we have an object, either the latest object in the existing list or a new object
                                if (property.objectType.isPresent()) {
                                    valueContext = property.objectType.get();
                                } else {
                                    valueContext = new SchemaObject();
                                    valueContext.id = "type_" + (typeIdx.getAndIncrement()); // TODO how can we get proper type names?
                                    property.objectType = Optional.of(valueContext);
                                    if (!htmlNameSections.isEmpty())
                                        property.title = Optional.ofNullable(htmlNameSections.get(Math.min(objectLevel, htmlNameSections.size() - 1)));
                                }
                                objectLevel++;
                            }
                        }
                    });
        }
        return featureTypeObject;
    }

}
