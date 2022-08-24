/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

// TODO this is a copy from Tiles

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableSeedingOptions.Builder.class)
public interface SeedingOptions {

  /**
   * @langEn If disabled the seeding will not be run when the API starts.
   * @langDe Steuert, ob das Seeding beim Start einer API ausgeführt wird.
   * @default `true`
   */
  @Nullable
  Boolean getRunOnStartup();

  @Value.Lazy
  @JsonIgnore
  default boolean shouldRunOnStartup() {
    return !Objects.equals(getRunOnStartup(), false);
  }

  /**
   * @langEn A crontab pattern to run the seeding periodically. There will only ever be one seeding
   *     in progress, so if the next run is scheduled before the last one finished, it will be
   *     skipped.
   * @langDe Ein Crontab-Pattern für die regelmäßige Ausführung des Seedings. Das Seeding wird stets
   *     nur einmal pro API zur gleichen Zeit ausgeführt, d.h. falls eine weitere Ausführung
   *     ansteht, während die vorherige noch läuft, wird diese übersprungen.
   * @default `null`
   */
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

  /**
   * @langEn If enabled the tile cache will be purged before the seeding starts.
   * @langDe Steuert, ob der Cache vor dem Seeding bereinigt wird.
   * @default `false`
   */
  @Nullable
  Boolean getPurge();

  @Value.Lazy
  @JsonIgnore
  default boolean shouldPurge() {
    return Objects.equals(getPurge(), true);
  }

  /**
   * @langEn The maximum number of threads the seeding is allowed to use. The actual number of
   *     threads used depends on the number of available background task threads when the seeding is
   *     about to start. If you want to allow more than thread, first check if sufficient background
   *     task threads are configured. Take into account that the seeding for multiple APIs will
   *     compete for the available background task threads.
   * @langDe Die maximale Anzahl an Threads, die für das Seeding verwendet werden darf. Die
   *     tatsächlich verwendete Zahl der Threads hängt davon ab, wie viele Threads für
   *     [Hintergrundprozesse](../../global-configuration.md#background-tasks) zur Verfügung stehen,
   *     wenn das Seeding startet. Wenn mehr als ein Thread erlaubt sein soll, ist zunächst zu
   *     prüfen, ob genügend Threads für
   *     [Hintergrundprozesse](../../global-configuration.md#background-tasks) konfiguriert sind. Es
   *     ist zu berücksichtigen, dass alle APIs um die vorhandenen Threads für
   *     [Hintergrundprozesse](../../global-configuration.md#background-tasks) konkurrieren.
   * @default `1`
   */
  @Nullable
  Integer getMaxThreads();

  @Value.Lazy
  @JsonIgnore
  default int getEffectiveMaxThreads() {
    return Objects.isNull(getMaxThreads()) || getMaxThreads() <= 1 ? 1 : getMaxThreads();
  }
}
