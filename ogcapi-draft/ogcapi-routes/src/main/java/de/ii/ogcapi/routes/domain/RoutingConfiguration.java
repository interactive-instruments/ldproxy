/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutingConfiguration.Builder.class)
public interface RoutingConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Nullable
  String getFeatureType();

  @Nullable
  Boolean getManageRoutes();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isManageRoutesEnabled() {
    return Objects.equals(getManageRoutes(), true);
  }

  @Nullable
  Boolean getIntermediateWaypoints();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsIntermediateWaypoints() {
    return Objects.equals(getIntermediateWaypoints(), true);
  }

  @Nullable
  Boolean getWeightRestrictions();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsWeightRestrictions() {
    return Objects.equals(getWeightRestrictions(), true);
  }

  @Nullable
  Boolean getHeightRestrictions();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsHeightRestrictions() {
    return Objects.equals(getHeightRestrictions(), true);
  }

  @Nullable
  Boolean getObstacles();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsObstacles() {
    return Objects.equals(getObstacles(), true);
  }

  @Nullable
  String getSpeedLimitUnit();

  String getDefaultPreference();

  String getDefaultMode();

  Map<String, RoutingFlag> getAdditionalFlags();

  @Nullable
  FeaturesCoreConfiguration.DefaultCrs getDefaultCrs();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default EpsgCrs getDefaultEpsgCrs() {
    return ImmutableEpsgCrs.copyOf(
        getDefaultCrs() == FeaturesCoreConfiguration.DefaultCrs.CRS84h
            ? OgcCrs.CRS84h
            : OgcCrs.CRS84);
  }

  Map<String, Integer> getCoordinatePrecision();

  @Nullable
  Double getElevationProfileSimplificationTolerance();

  @Nullable
  HtmlForm getHtml();

  @Override
  default Builder getBuilder() {
    return new ImmutableRoutingConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableRoutingConfiguration.Builder builder =
        ((ImmutableRoutingConfiguration.Builder) source.getBuilder()).from(source).from(this);

    RoutingConfiguration src = (RoutingConfiguration) source;

    // always override the default configuration options
    builder.additionalFlags(getAdditionalFlags());

    return builder.build();
  }
}
