/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableSeedingOptions.Builder.class)
public interface SeedingOptions {

  @Nullable
  Boolean getRunOnStartup();

  @Value.Lazy
  @JsonIgnore
  default boolean shouldRunOnStartup() {
    return !Objects.equals(getRunOnStartup(), false);
  }

  @Nullable
  String getRunPeriodic();

  @Value.Lazy
  @JsonIgnore
  default boolean shouldRunPeriodic() {
    return Objects.nonNull(getRunPeriodic());
  }

  @Value.Lazy
  @JsonIgnore
  default Optional<String> getCronExpression() {
    return Optional.ofNullable(getRunPeriodic());
  }

  @Nullable
  Boolean getPurge();

  @Value.Lazy
  @JsonIgnore
  default boolean shouldPurge() {
    return Objects.equals(getPurge(), true);
  }

  @Nullable
  Integer getMaxThreads();

  @Value.Lazy
  @JsonIgnore
  default int getEffectiveMaxThreads() {
    return Objects.isNull(getMaxThreads()) || getMaxThreads() <= 1 ? 1 : getMaxThreads();
  }

}
