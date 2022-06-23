/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureType;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
public interface FeatureTransformationContext {
  enum Event {
    START,
    END,
    FEATURE_START,
    FEATURE_END,
    PROPERTY,
    COORDINATES,
    GEOMETRY_END,
    ARRAY_START,
    OBJECT_START,
    OBJECT_END,
    ARRAY_END
  }

  OgcApi getApi();

  OgcApiDataV2 getApiData();

  String getCollectionId();

  Optional<FeatureSchema> getFeatureSchema();

  OutputStream getOutputStream();

  Optional<CrsTransformer> getCrsTransformer();

  Optional<EpsgCrs> getSourceCrs();

  EpsgCrs getDefaultCrs();

  @Value.Derived
  default EpsgCrs getTargetCrs() {
    if (getCrsTransformer().isPresent()) return getCrsTransformer().get().getTargetCrs();
    return getSourceCrs().orElse(getDefaultCrs());
  }
  ;

  List<Link> getLinks();

  boolean isFeatureCollection();

  Map<String, Codelist> getCodelists();

  @Value.Default
  default boolean getShowsFeatureSelfLink() {
    return true;
  }

  @Value.Default
  default boolean isHitsOnly() {
    return false;
  }

  @Value.Default
  default boolean isHitsOnlyIfMore() {
    return false;
  }

  @Value.Default
  default boolean isPropertyOnly() {
    return false;
  }

  @Value.Default
  default List<String> getFields() {
    return ImmutableList.of("*");
  }

  ApiRequestContext getOgcApiRequest();

  int getLimit();

  int getOffset();

  @Value.Derived
  default int getPage() {
    return getLimit() > 0 ? (getLimit() + getOffset()) / getLimit() : 0;
  }

  Optional<Locale> getLanguage();

  Optional<I18n> getI18n();

  @Nullable
  State getState();

  // to ValueTransformerContext
  @Value.Derived
  default String getServiceUrl() {
    return getOgcApiRequest()
        .getUriCustomizer()
        .copy()
        .cutPathAfterSegments(getApiData().getSubPath().toArray(new String[0]))
        .clearParameters()
        .toString();
  }

  @Value.Derived
  @Value.Auxiliary
  default Optional<FeatureTypeConfigurationOgcApi> getCollection() {
    return Optional.ofNullable(getApiData().getCollections().get(getCollectionId()));
  }

  // TODO: to geometry simplification module
  @Value.Default
  default double getMaxAllowableOffset() {
    return 0;
  }

  @Value.Default
  default boolean shouldSwapCoordinates() {
    return false;
  }

  @Value.Default
  default List<Integer> getGeometryPrecision() {
    return ImmutableList.of(0, 0, 0);
  }

  abstract class State {
    public abstract Event getEvent();

    public abstract OptionalLong getNumberReturned();

    public abstract OptionalLong getNumberMatched();

    public abstract Optional<FeatureType> getCurrentFeatureType();

    public abstract Optional<FeatureProperty> getCurrentFeatureProperty();

    public abstract List<Integer> getCurrentMultiplicity();

    public abstract Optional<String> getCurrentValue();
  }

  default <T extends ExtensionConfiguration> T getConfiguration(Class<T> clazz) {
    return Optional.ofNullable(getApiData().getCollections().get(getCollectionId()))
        .flatMap(featureTypeConfiguration -> featureTypeConfiguration.getExtension(clazz))
        .or(() -> getApiData().getExtension(clazz))
        .orElseThrow();
  }
}
