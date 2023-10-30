/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entities.domain.Mergeable;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Deprecated(since = "3.4.0")
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesCollectionQueryables.Builder.class)
public interface FeaturesCollectionQueryables
    extends Buildable<FeaturesCollectionQueryables>, Mergeable<FeaturesCollectionQueryables> {

  abstract class Builder implements BuildableBuilder<FeaturesCollectionQueryables> {}

  @Override
  default FeaturesCollectionQueryables.Builder getBuilder() {
    return new ImmutableFeaturesCollectionQueryables.Builder().from(this);
  }

  static FeaturesCollectionQueryables of() {
    return new ImmutableFeaturesCollectionQueryables.Builder().build();
  }

  @Deprecated(since = "3.4.0")
  List<String> getSpatial();

  @Deprecated(since = "3.4.0")
  List<String> getTemporal();

  @Deprecated(since = "3.3.0")
  List<String> getQ();

  @Deprecated(since = "3.4.0")
  List<String> getOther();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<String> getAll() {
    return ImmutableList.<String>builder()
        .addAll(getSpatial())
        .addAll(getTemporal())
        .addAll(getQ())
        .addAll(getOther())
        .build();
  }

  @Override
  default FeaturesCollectionQueryables mergeInto(FeaturesCollectionQueryables source) {
    return new ImmutableFeaturesCollectionQueryables.Builder()
        .from(source)
        .from(this)
        .spatial(
            Stream.concat(source.getSpatial().stream(), getSpatial().stream())
                .distinct()
                .collect(Collectors.toList()))
        .temporal(
            Stream.concat(source.getTemporal().stream(), getTemporal().stream())
                .distinct()
                .collect(Collectors.toList()))
        .other(
            Stream.concat(source.getOther().stream(), getOther().stream())
                .distinct()
                .collect(Collectors.toList()))
        .build();
  }
}
