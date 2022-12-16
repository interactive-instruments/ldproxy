/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.app;

import com.google.common.collect.ImmutableList;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ChangingItemCount;
import de.ii.ogcapi.foundation.domain.ChangingLastModified;
import de.ii.ogcapi.foundation.domain.ChangingSpatialExtent;
import de.ii.ogcapi.foundation.domain.ChangingTemporalExtent;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.services.domain.AbstractService;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OgcApiEntity extends AbstractService<OgcApiDataV2> implements OgcApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEntity.class);

  private final CrsTransformerFactory crsTransformerFactory;
  private final ExtensionRegistry extensionRegistry;
  private final ServicesContext servicesContext;

  @AssistedInject
  public OgcApiEntity(
      CrsTransformerFactory crsTransformerFactory,
      ExtensionRegistry extensionRegistry,
      ServicesContext servicesContext,
      @Assisted OgcApiDataV2 data) {
    super(data);
    this.crsTransformerFactory = crsTransformerFactory;
    this.extensionRegistry = extensionRegistry;
    this.servicesContext = servicesContext;
  }

  @Override
  protected boolean onStartup() throws InterruptedException {

    // validate the API, the behaviour depends on the validation option for the API:
    // NONE: no validation
    // LAX: invalid the configuration and try to remove invalid options, but try to start the
    // service with the valid options
    // STRICT: no validation during hydration, validation will be done in onStartup() and startup
    // will fail in case of any error
    boolean isSuccess = true;
    OgcApiDataV2 apiData = getData();
    MODE apiValidation = apiData.getApiValidation();

    if (apiValidation != MODE.NONE && LOGGER.isInfoEnabled()) {
      LOGGER.info("Validating service '{}'.", apiData.getId());
    }

    for (ApiExtension extension : extensionRegistry.getExtensions()) {
      if (extension.isEnabledForApi(apiData)) {
        ValidationResult result = extension.onStartup(this, apiValidation);
        isSuccess = isSuccess && result.isSuccess();
        result.getErrors().forEach(LOGGER::error);
        result
            .getStrictErrors()
            .forEach(result.getMode() == MODE.STRICT ? LOGGER::error : LOGGER::warn);
        result.getWarnings().forEach(LOGGER::warn);
      }
      checkForStartupCancel();
    }

    if (!isSuccess && LOGGER.isErrorEnabled()) {
      LOGGER.error(
          "Service with id '{}' could not be started. See previous log messages for reasons.",
          apiData.getId());
    }

    return isSuccess;
  }

  @Override
  public <T extends FormatExtension> Optional<T> getOutputFormat(
      Class<T> extensionType, ApiMediaType mediaType, String path, Optional<String> collectionId) {
    List<T> candidates =
        extensionRegistry.getExtensionsForType(extensionType).stream()
            .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
            .filter(
                outputFormatExtension ->
                    collectionId
                        .map(s -> outputFormatExtension.isEnabledForApi(getData(), s))
                        .orElseGet(() -> outputFormatExtension.isEnabledForApi(getData())))
            .collect(Collectors.toUnmodifiableList());
    MediaType selected =
        ApiMediaType.negotiateMediaType(
            ImmutableList.of(mediaType.type()),
            candidates.stream()
                .map(f -> f.getMediaType().type())
                .collect(Collectors.toUnmodifiableList()));
    return candidates.stream().filter(f -> f.getMediaType().type().equals(selected)).findFirst();
  }

  @Override
  public <T extends FormatExtension> List<T> getAllOutputFormats(
      Class<T> extensionType, ApiMediaType mediaType, String path, Optional<T> excludeFormat) {
    return extensionRegistry.getExtensionsForType(extensionType).stream()
        .filter(
            outputFormatExtension ->
                !Objects.equals(outputFormatExtension, excludeFormat.orElse(null)))
        .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
        .filter(
            outputFormatExtension ->
                mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
        .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(getData()))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent() {
    return getChangingData().get(ChangingSpatialExtent.class).map(ChangingValue::getValue);
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(EpsgCrs targetCrs) {
    Optional<BoundingBox> spatialExtent = getSpatialExtent();

    if (spatialExtent.isPresent()) {
      return transformSpatialExtent(spatialExtent.get(), targetCrs);
    }

    return Optional.empty();
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String collectionId) {
    return getChangingData()
        .get(ChangingSpatialExtent.class, collectionId)
        .map(ChangingValue::getValue);
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String collectionId, EpsgCrs targetCrs) {
    Optional<BoundingBox> spatialExtent = getSpatialExtent(collectionId);

    if (spatialExtent.isPresent()) {
      return transformSpatialExtent(spatialExtent.get(), targetCrs);
    }

    return Optional.empty();
  }

  @Override
  public boolean updateSpatialExtent(String collectionId, BoundingBox bbox) {
    return getChangingData()
        .update(ChangingSpatialExtent.class, collectionId, ChangingSpatialExtent.of(bbox));
  }

  @Override
  public Optional<TemporalExtent> getTemporalExtent() {
    return getChangingData().get(ChangingTemporalExtent.class).map(ChangingValue::getValue);
  }

  @Override
  public Optional<TemporalExtent> getTemporalExtent(String collectionId) {
    return getChangingData()
        .get(ChangingTemporalExtent.class, collectionId)
        .map(ChangingValue::getValue);
  }

  @Override
  public boolean updateTemporalExtent(String collectionId, TemporalExtent temporalExtent) {
    return getChangingData()
        .update(
            ChangingTemporalExtent.class, collectionId, ChangingTemporalExtent.of(temporalExtent));
  }

  @Override
  public Optional<Instant> getLastModified() {
    return getChangingData().get(ChangingLastModified.class).map(ChangingValue::getValue);
  }

  @Override
  public Optional<Instant> getLastModified(String collectionId) {
    return getChangingData()
        .get(ChangingLastModified.class, collectionId)
        .map(ChangingValue::getValue);
  }

  @Override
  public boolean updateLastModified(String collectionId, Instant lastModified) {
    return getChangingData()
        .update(ChangingLastModified.class, collectionId, ChangingLastModified.of(lastModified));
  }

  @Override
  public Optional<Long> getItemCount() {
    return getChangingData().get(ChangingItemCount.class).map(ChangingValue::getValue);
  }

  @Override
  public Optional<Long> getItemCount(String collectionId) {
    return getChangingData()
        .get(ChangingItemCount.class, collectionId)
        .map(ChangingValue::getValue);
  }

  @Override
  public boolean updateItemCount(String collectionId, Long itemCount) {
    return getChangingData()
        .update(ChangingItemCount.class, collectionId, ChangingItemCount.of(itemCount));
  }

  @Override
  public URI getUri() {
    return servicesContext.getUri().resolve(String.join("/", getData().getSubPath()));
  }

  private Optional<BoundingBox> transformSpatialExtent(
      BoundingBox spatialExtent, EpsgCrs targetCrs) {
    if (Objects.nonNull(spatialExtent)) {
      Optional<CrsTransformer> crsTransformer =
          spatialExtent.is3d()
              ? crsTransformerFactory.getTransformer(OgcCrs.CRS84h, targetCrs)
              : crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

      if (crsTransformer.isPresent()) {
        try {
          return Optional.ofNullable(crsTransformer.get().transformBoundingBox(spatialExtent));
        } catch (CrsTransformationException e) {
          LOGGER.error(String.format("Error converting bounding box to CRS %s.", targetCrs));
        }
      }
    }

    return Optional.ofNullable(spatialExtent);
  }
}
