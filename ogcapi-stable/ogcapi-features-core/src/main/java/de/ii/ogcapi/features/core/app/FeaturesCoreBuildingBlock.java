/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.DEFAULT_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MAX_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MINIMUM_PAGE_SIZE;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaArray;
import de.ii.ogcapi.features.core.domain.JsonSchemaBoolean;
import de.ii.ogcapi.features.core.domain.JsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.JsonSchemaNumber;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.core.domain.JsonSchemaOneOf;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.JsonSchemaRefExternal;
import de.ii.ogcapi.features.core.domain.JsonSchemaString;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.CollectionExtent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.DatasetChangeListener;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Features Core
 * @langEn The module *Features Core* has to be enabled for every API with a feature provider. It
 *     provides the resources *Features* and *Feature*.
 *     <p>*Features Core* implements all requirements of conformance class *Core* of [OGC API -
 *     Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for
 *     the two mentioned resources.
 * @langDe Das Modul *Features Core* ist für jede über ldproxy bereitgestellte API mit einem
 *     Feature-Provider zu aktivieren. Es stellt die Ressourcen "Features" und "Feature" bereit.
 *     <p>"Features Core" implementiert alle Vorgaben der Konformitätsklasse "Core" von [OGC API -
 *     Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für
 *     die zwei genannten Ressourcen.
 * @example {@link de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration}
 * @endpointTable {@link de.ii.ogcapi.features.core.app.EndpointFeatures}
 * @queryParameterTable {@link de.ii.ogcapi.features.core.app.QueryParameterBbox}, {@link
 *     de.ii.ogcapi.features.core.app.QueryParameterDatetime}, {@link
 *     de.ii.ogcapi.features.core.app.QueryParameterFFeatures}, {@link
 *     de.ii.ogcapi.features.core.app.QueryParameterLimitFeatures}, {@link
 *     de.ii.ogcapi.features.core.app.QueryParameterOffsetFeatures}
 */
@Singleton
@AutoBind
public class FeaturesCoreBuildingBlock implements ApiBuildingBlock {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesCoreBuildingBlock.class);

  private final FeaturesCoreProviders providers;
  private final CrsTransformerFactory crsTransformerFactory;
  private final ClassSchemaCache classSchemaCache;

  @Inject
  public FeaturesCoreBuildingBlock(
      FeaturesCoreProviders providers,
      CrsTransformerFactory crsTransformerFactory,
      ClassSchemaCache classSchemaCache) {
    this.providers = providers;
    this.crsTransformerFactory = crsTransformerFactory;
    this.classSchemaCache = classSchemaCache;
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableFeaturesCoreConfiguration.Builder()
        .enabled(true)
        .itemType(FeaturesCoreConfiguration.ItemType.feature)
        .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84)
        .minimumPageSize(MINIMUM_PAGE_SIZE)
        .defaultPageSize(DEFAULT_PAGE_SIZE)
        .maximumPageSize(MAX_PAGE_SIZE)
        .validateCoordinatesInQueries(false)
        .showsFeatureSelfLink(false)
        .build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    OgcApiDataV2 apiData = api.getData();

    apiData
        .getCollections()
        .keySet()
        .forEach(
            collectionId ->
                initMetadata(api, collectionId, Instant.now().truncatedTo(ChronoUnit.SECONDS)));

    providers
        .getFeatureProvider(apiData)
        .ifPresent(
            provider -> {
              provider.getChangeHandler().addListener(onDatasetChange(api));
              provider.getChangeHandler().addListener(onFeatureChange(api));
            });

    // register schemas that cannot be derived automatically
    Schema<?> stringSchema = classSchemaCache.getSchema(JsonSchemaString.class);
    Schema<?> numberSchema = classSchemaCache.getSchema(JsonSchemaNumber.class);
    Schema<?> integerSchema = classSchemaCache.getSchema(JsonSchemaInteger.class);
    Schema<?> booleanSchema = classSchemaCache.getSchema(JsonSchemaBoolean.class);
    Schema<?> objectSchema = classSchemaCache.getSchema(JsonSchemaObject.class);
    Schema<?> arraySchema = classSchemaCache.getSchema(JsonSchemaArray.class);
    Schema<?> refSchema = classSchemaCache.getSchema(JsonSchemaRef.class);
    Schema<?> refExternalSchema = classSchemaCache.getSchema(JsonSchemaRefExternal.class);
    Schema<?> oneOfSchema = classSchemaCache.getSchema(JsonSchemaOneOf.class);
    classSchemaCache.registerSchema(
        JsonSchema.class,
        new ComposedSchema()
            .addOneOfItem(stringSchema)
            .addOneOfItem(numberSchema)
            .addOneOfItem(integerSchema)
            .addOneOfItem(booleanSchema)
            .addOneOfItem(objectSchema)
            .addOneOfItem(arraySchema)
            .addOneOfItem(refSchema)
            .addOneOfItem(refExternalSchema)
            .addOneOfItem(oneOfSchema),
        ImmutableList.of(
            JsonSchemaString.class,
            JsonSchemaNumber.class,
            JsonSchemaInteger.class,
            JsonSchemaBoolean.class,
            JsonSchemaObject.class,
            JsonSchemaArray.class,
            JsonSchemaRef.class,
            JsonSchemaOneOf.class));

    return ValidationResult.of();
  }

  // TODO: add capability to periodically reinitialize metadata from the feature data (to account
  // for lost notifications,
  //       because extent changes because of deletes are not taken into account, etc.)
  // initialize dynamic collection metadata
  private void initMetadata(OgcApi api, String collectionId, Instant lastModified) {
    OgcApiDataV2 apiData = api.getData();
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    final Optional<CollectionExtent> optionalExtent = apiData.getExtent(collectionId);

    optionalExtent
        .flatMap(extent -> extent.isSpatialComputed() ? Optional.empty() : extent.getSpatial())
        .or(() -> computeBbox(apiData, collectionId))
        .ifPresent(bbox -> api.updateSpatialExtent(collectionId, bbox));

    optionalExtent
        .flatMap(extent -> extent.isTemporalComputed() ? Optional.empty() : extent.getTemporal())
        .or(() -> computeInterval(apiData, collectionId))
        .ifPresent(interval -> api.updateTemporalExtent(collectionId, interval));

    final Optional<FeatureProvider2> provider =
        providers.getFeatureProvider(apiData, collectionData);
    if (provider.map(FeatureProvider2::supportsQueries).orElse(false)) {
      final String featureTypeId =
          collectionData
              .getExtension(FeaturesCoreConfiguration.class)
              .map(cfg -> cfg.getFeatureType().orElse(collectionId))
              .orElse(collectionId);
      final long count = ((FeatureQueries) provider.get()).getFeatureCount(featureTypeId);
      api.updateItemCount(collectionId, count);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Number of items in collection '{}': {}", collectionId, count);
      }

      api.updateLastModified(collectionId, lastModified);
    }
  }

  private DatasetChangeListener onDatasetChange(OgcApi api) {
    return change -> {
      for (String featureType : change.getFeatureTypes()) {
        String collectionId = getCollectionId(api.getData().getCollections().values(), featureType);

        initMetadata(api, collectionId, change.getModified());
      }
    };
  }

  private FeatureChangeListener onFeatureChange(OgcApi api) {
    return change -> {
      String collectionId =
          getCollectionId(api.getData().getCollections().values(), change.getFeatureType());
      switch (change.getAction()) {
        case CREATE:
          api.updateItemCount(collectionId, (long) change.getFeatureIds().size());
          change
              .getBoundingBox()
              .flatMap(this::transformToCrs84)
              .ifPresent(bbox -> api.updateSpatialExtent(collectionId, bbox));
          change
              .getInterval()
              .ifPresent(
                  interval -> api.updateTemporalExtent(collectionId, TemporalExtent.of(interval)));
          break;
        case UPDATE:
          change
              .getBoundingBox()
              .flatMap(this::transformToCrs84)
              .ifPresent(bbox -> api.updateSpatialExtent(collectionId, bbox));
          change
              .getInterval()
              .ifPresent(
                  interval -> api.updateTemporalExtent(collectionId, TemporalExtent.of(interval)));
          break;
        case DELETE:
          api.updateItemCount(collectionId, (long) -change.getFeatureIds().size());
          break;
      }
      api.updateLastModified(collectionId, change.getModified());
    };
  }

  private Optional<BoundingBox> transformToCrs84(BoundingBox boundingBox) {
    if (!boundingBox.getEpsgCrs().equals(OgcCrs.CRS84)) {
      Optional<CrsTransformer> transformer =
          crsTransformerFactory.getTransformer(boundingBox.getEpsgCrs(), OgcCrs.CRS84);
      if (transformer.isPresent()) {
        try {
          return Optional.ofNullable(transformer.get().transformBoundingBox(boundingBox));
        } catch (CrsTransformationException e) {
          LOGGER.error(
              "Error while transforming the spatial extent of a feature to CRS84: {}",
              e.getMessage());
        }
      }
      return Optional.empty();
    }
    return Optional.ofNullable(boundingBox);
  }

  private Optional<BoundingBox> computeBbox(OgcApiDataV2 apiData, String collectionId) {

    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
    Optional<FeatureProvider2> featureProvider =
        providers.getFeatureProvider(apiData, collectionData);

    if (featureProvider.map(FeatureProvider2::supportsExtents).orElse(false)) {
      String featureType =
          collectionData
              .getExtension(FeaturesCoreConfiguration.class)
              .flatMap(FeaturesCoreConfiguration::getFeatureType)
              .orElse(collectionId);
      Optional<BoundingBox> spatialExtent =
          featureProvider.get().extents().getSpatialExtent(featureType);
      if (spatialExtent.isPresent()) {

        BoundingBox boundingBox = spatialExtent.get();
        if (!boundingBox.getEpsgCrs().equals(OgcCrs.CRS84)
            && !boundingBox.getEpsgCrs().equals(OgcCrs.CRS84h)) {
          Optional<CrsTransformer> transformer =
              boundingBox.is3d()
                  ? crsTransformerFactory.getTransformer(boundingBox.getEpsgCrs(), OgcCrs.CRS84h)
                  : crsTransformerFactory.getTransformer(boundingBox.getEpsgCrs(), OgcCrs.CRS84);
          if (transformer.isPresent()) {
            try {
              boundingBox = transformer.get().transformBoundingBox(boundingBox);
            } catch (CrsTransformationException e) {
              LOGGER.error(
                  "Error while computing spatial extent of collection '{}' while transforming the CRS of the bounding box: {}",
                  collectionId,
                  e.getMessage());
              return Optional.empty();
            }
          }
        }

        return Optional.of(boundingBox);
      }
    }

    return Optional.empty();
  }

  private Optional<TemporalExtent> computeInterval(OgcApiDataV2 apiData, String collectionId) {
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
    Optional<FeatureProvider2> featureProvider =
        providers.getFeatureProvider(apiData, collectionData);

    if (featureProvider.map(FeatureProvider2::supportsExtents).orElse(false)) {
      String featureType =
          collectionData
              .getExtension(FeaturesCoreConfiguration.class)
              .flatMap(FeaturesCoreConfiguration::getFeatureType)
              .orElse(collectionId);

      return featureProvider.get().extents().getTemporalExtent(featureType).map(TemporalExtent::of);
    }
    return Optional.empty();
  }

  private String getCollectionId(
      Collection<FeatureTypeConfigurationOgcApi> collections, String featureType) {
    return collections.stream()
        .map(
            collection ->
                collection
                    .getExtension(FeaturesCoreConfiguration.class)
                    .flatMap(FeaturesCoreConfiguration::getFeatureType))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(ft -> Objects.equals(ft, featureType))
        .findFirst()
        .orElse(featureType);
  }
}
