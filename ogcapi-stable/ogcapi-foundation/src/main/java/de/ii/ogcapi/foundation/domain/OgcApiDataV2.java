/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import org.immutables.value.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @title  API
 * @en Each API represents a deployment of a single OGC Web API.
 * @de Jede API stellt eine OGC Web API bereit.
 *
 * Die Konfiguration einer API wird in einer Konfigurationsdatei in einem Objekt mit den folgenden
 * Eigenschaften beschrieben.
 *
 * Informationen zu den einzelnen API-Modulen finden Sie [hier](building-blocks/README.md), siehe
 * `api` in der nachfolgenden Tabelle.
 * @see Metadata
 * @see ExternalDocumentation
 * @see CollectionExtent
 * @see MODE
 * @see ServiceData
 * @see FeatureTypeConfigurationOgcApi
 * @see FeatureTypeConfiguration
 */

/**
 * @title API modules
 * @en Modules might be configured for the API or for single collections. The final configuration is
 * formed by merging the following sources in this order:
 *
 * * The module defaults, see [API modules](building-blocks/README.md).
 * * Optional deployment defaults in the `defaults` directory.
 * * API level configuration.
 * * Collection level configuration.
 * * Optional deployment overrides in the `overrides` directory.
 * @de Ein Array dieser Modul-Konfigurationen steht auf der Ebene der gesamten API und für
 * jede Collection zur Verfügung. Die jeweils gültige Konfiguration ergibt sich aus der Priorisierung:
 *
 * * Ist nichts angegeben, dann gelten die im ldproxy-Code vordefinierten Standardwerte. Diese sind bei den jeweiligen [API-Modulen](building-blocks/README.md) spezifiziert.
 * * Diese systemseitigen Standardwerte können von den Angaben im Verzeichnis "defaults" überschrieben werden.
 * * Diese deploymentweiten Standardwerte können von den Angaben in der API-Definition auf Ebene der API überschrieben werden.
 * * Diese API-weiten Standardwerte können bei den Collection-Ressourcen und untergeordneten Ressourcen von den Angaben in der API-Definition auf Ebene der Collection überschrieben werden.
 * * Diese Werte können durch Angaben im Verzeichnis "overrides" überschrieben werden.
 */

/**
 * @title Example
 * @en See the
 * [API configuration](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/services/vineyards.yml)
 * of the API [Vineyards in Rhineland-Palatinate, Germany](https://demo.ldproxy.net/vineyards).
 * @de Als Beispiel siehe die
 * [API-Konfiguration](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/services/vineyards.yml)
 * der API [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
 */

/**
 * @title Storage
 * @en API configurations reside under the relative path `store/entities/services/{apiId}.yml` in the data directory.
 * @de API-Konfigurationen liegen unter dem relativen Pfad `store/entities/services/{apiId}.yml` im Datenverzeichnis.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiDataV2.Builder.class)
public abstract class OgcApiDataV2 implements ServiceData, ExtendableConfiguration {

    public static final String SERVICE_TYPE = "OGC_API";

    static abstract class Builder implements EntityDataBuilder<OgcApiDataV2> {

        // jackson should append to instead of replacing extensions
        @JsonIgnore
        public abstract Builder extensions(Iterable<? extends ExtensionConfiguration> elements);

        @JsonProperty("api")
        public abstract Builder addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

        public abstract ImmutableOgcApiDataV2.Builder id(String id);

        @Override
        public EntityDataBuilder<OgcApiDataV2> fillRequiredFieldsWithPlaceholders() {
            return this.id(EntityDataDefaults.PLACEHOLDER)
                       .serviceType(EntityDataDefaults.PLACEHOLDER);
        }

    }

    @Value.Derived
    @Override
    public long getEntitySchemaVersion() {
        return 2;
    }

    @Override
    public Optional<String> getEntitySubType() {
        return Optional.of(SERVICE_TYPE);
    }

    @Value.Default
    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    public abstract Optional<Metadata> getMetadata();

    public abstract Optional<ExternalDocumentation> getExternalDocs();

    public abstract Optional<CollectionExtent> getDefaultExtent();

    public abstract Optional<Caching> getDefaultCaching();

    @Value.Default
    public MODE getApiValidation() {
        return MODE.NONE;
    }

    /**
     * @en Tags for this API. Every tag is a string without white space. Tags are shown in
     * the *API Catalog* and can be used to filter the catalog response with the query parameter
     * `tags`, e.g. `tags=INSPIRE`.<br>_since version 2.1_
     * @de Ordnet der API die aufgelisteten Tags zu. Die Tags müssen jeweils Strings ohne
     * Leerzeichen sein. Die Tags werden im API-Katalog angezeigt und können über den
     * Query-Parameter `tags` zur Filterung der in der API-Katalog-Antwort zurückgelieferten
     * APIs verwendet werden, z.B. `tags=INSPIRE`.<br>_seit Version 2.1_
     * @default `null`
     */
    // TODO: move to ServiceData?
    public abstract List<String> getTags();

    // TODO: move to ServiceData?
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public List<String> getSubPath() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        builder.add(getId());
        if (getApiVersion().isPresent())
            builder.add("v"+getApiVersion().get());
        return builder.build();
    }

    @JsonProperty(value = "api")
    @JsonMerge
    @Override
    public abstract List<ExtensionConfiguration> getExtensions();

    /**
     * @en Collection configurations, the key is the collection id, for the value see
     * [Collection](#collection) below.
     * @de Ein Objekt mit der spezifischen Konfiguration zu jeder Objektart, der Name der Objektart
     * ist der Schlüssel, der Wert ein [Collection-Objekt](#collection).
     * @default `{}`
     */
    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>,
    // but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    public abstract BuildableMap<FeatureTypeConfigurationOgcApi,
            ImmutableFeatureTypeConfigurationOgcApi.Builder> getCollections();

    public Optional<FeatureTypeConfigurationOgcApi> getCollectionData(String collectionId) {
        return Optional.ofNullable(getCollections().get(collectionId));
    }

    @Override
    @Value.Derived
    public boolean isLoading() {
        //TODO: delegate to extensions?
        return false;
    }

    @Override
    @Value.Derived
    public boolean hasError() {
        //TODO: delegate to extensions?
        return false;
    }

    @Value.Check
    public OgcApiDataV2 mergeBuildingBlocks() {
        List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

        // remove duplicates
        if (getExtensions().size() > distinctExtensions.size()) {
            return new ImmutableOgcApiDataV2.Builder().from(this)
                                                      .extensions(distinctExtensions)
                                                      .build();
        }

        boolean collectionsHaveMissingParentExtensions = getCollections().values()
                                                                         .stream()
                                                                         .anyMatch(collection -> collection.getParentExtensions()
                                                                                                           .size() < getMergedExtensions().size());

        if (collectionsHaveMissingParentExtensions) {
            Map<String, FeatureTypeConfigurationOgcApi> mergedCollections = new LinkedHashMap<>(getCollections());

                mergedCollections.values()
                    .forEach(featureTypeConfigurationOgcApi -> mergedCollections
                        .put(featureTypeConfigurationOgcApi.getId(),
                            featureTypeConfigurationOgcApi.getBuilder()
                                                                                                                                                                   .parentExtensions(getMergedExtensions())
                                .build()));

            return new ImmutableOgcApiDataV2.Builder().from(this)
                                                      .collections(mergedCollections)
                                                      .build();
        }

        return this;
    }

    public boolean isCollectionEnabled(final String collectionId) {
        return getCollections().containsKey(collectionId) && getCollections().get(collectionId)
                                                                             .getEnabled();
    }

    @JsonIgnore
    @Value.Derived
    public boolean isDataset() {
        // return false if there no collection or no collection that is enabled
        return getCollections()
            .values()
            .stream()
            .anyMatch(FeatureTypeConfigurationOgcApi::getEnabled);
    }

    /**
     * Determine spatial extent of all collections in the dataset.
     *
     * @return the bounding box in the default CRS
     */
    @JsonIgnore
    @Value.Derived
    public Optional<BoundingBox> getSpatialExtent() {
        return getCollections().keySet()
                               .stream()
                               .map(this::getSpatialExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(BoundingBox::toArray)
                               .reduce((doubles, doubles2) -> new double[]{
                                       Math.min(doubles[0], doubles2[0]),
                                       Math.min(doubles[1], doubles2[1]),
                                       Math.max(doubles[2], doubles2[2]),
                                       Math.max(doubles[3], doubles2[3])})
                               .map(doubles -> BoundingBox.of(doubles[0], doubles[1], doubles[2], doubles[3], OgcCrs.CRS84));
    }

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     *
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs             the target CRS
     * @return the bounding box
     */
    public Optional<BoundingBox> getSpatialExtent(CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent();

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), crsTransformerFactory, targetCrs));
        }

        return Optional.empty();
    }

    /**
     * Determine temporal extent of all collections in the dataset.
     *
     * @return the temporal extent in the Gregorian calendar
     */
    @JsonIgnore
    @Value.Derived
    public Optional<TemporalExtent> getTemporalExtent() {
        return getCollections().keySet()
                               .stream()
                               .map(this::getTemporalExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()})
                               .reduce((longs, longs2) -> new Long[]{
                                       longs[0] == null || longs2[0] == null ? null : Math.min(longs[0], longs2[0]),
                                       longs[1] == null || longs2[1] == null ? null : Math.max(longs[1], longs2[1])})
                               .map(longs -> new ImmutableTemporalExtent.Builder().start(longs[0])
                                                                                  .end(longs[1])
                                                                                  .build());
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    public Optional<BoundingBox> getSpatialExtent(String collectionId) {
        return getExtent(collectionId).flatMap(CollectionExtent::getSpatial);
    }

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     *
     * @param collectionId          the name of the feature type
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs             the target CRS
     * @return the bounding box in the target CRS
     */
    public Optional<BoundingBox> getSpatialExtent(String collectionId, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent(collectionId);

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), crsTransformerFactory, targetCrs));
        }

        return Optional.empty();
    }

    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs, true);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get()
                                 .transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }

    /**
     * Determine temporal extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the temporal extent in the Gregorian calendar
     */
    public Optional<TemporalExtent> getTemporalExtent(String collectionId) {
        return getExtent(collectionId).flatMap(CollectionExtent::getTemporal);
    }

    /**
     * Determine extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the extent
     */
    public Optional<CollectionExtent> getExtent(String collectionId) {
        return getCollections().values()
            .stream()
            .filter(featureTypeConfiguration -> featureTypeConfiguration.getId()
                .equals(collectionId))
            .filter(FeatureTypeConfigurationOgcApi::getEnabled)
            .findFirst()
            .flatMap(FeatureTypeConfigurationOgcApi::getExtent)
            .flatMap(collectionExtent -> mergeExtents(getDefaultExtent(), Optional.of(collectionExtent)))
            .or(this::getDefaultExtent);
    }

    private Optional<CollectionExtent> mergeExtents(Optional<CollectionExtent> defaultExtent, Optional<CollectionExtent> collectionExtent) {
        if (defaultExtent.isEmpty())
            return collectionExtent;
        else if (collectionExtent.isEmpty())
            return defaultExtent;

        return Optional.of(new ImmutableCollectionExtent.Builder()
            .from(defaultExtent.get())
            .from(collectionExtent.get())
            .build());
    }

    public <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz, String collectionId) {
        if (isCollectionEnabled(collectionId)) {
            return getCollections().get(collectionId).getExtension(clazz);
        }
        return getExtension(clazz);
    }
}
