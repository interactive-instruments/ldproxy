/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock CRUD
 * @buildingBlockAlias TRANSACTIONAL
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: CRUD
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "CRUD", aliases = "TRANSACTIONS")
@JsonDeserialize(builder = ImmutableCrudConfiguration.Builder.class)
public interface CrudConfiguration extends ExtensionConfiguration {

  /**
   * @langEn Option to enable support for conditional processing of PUT, PATCH, and DELETE requests,
   *     based on the time when the feature was last updated. Such requests must include an
   *     `If-Unmodified-Since` header, otherwise they will be rejected. A feature will only be
   *     changed, if the feature was not changed since the timestamp in the header (or if no last
   *     modification time is known for the feature).
   *     <p>The setting is ignored, if `optimisticLockingETag` is enabled.
   * @langDe Option zur Aktivierung der Unterstützung für die bedingte Verarbeitung von PUT-, PATCH-
   *     und DELETE-Anfragen, basierend auf der Zeit, zu der das Feature zuletzt aktualisiert wurde.
   *     Solche Anfragen müssen einen `If-Unmodified-Since`-Header enthalten, andernfalls werden sie
   *     zurückgewiesen. Ein Feature wird nur dann geändert, wenn das Feature seit dem Zeitstempel
   *     im Header nicht geändert wurde (oder wenn kein letzter Änderungszeitpunkt für das Feature
   *     bekannt ist).
   *     <p>Die Option wird ignoriert, wenn `optimisticLockingETag` aktiviert ist.
   * @default false
   * @since v3.5
   */
  @Nullable
  Boolean getOptimisticLockingLastModified();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsLastModified() {
    return Objects.equals(getOptimisticLockingLastModified(), true);
  }

  /**
   * @langEn Option to enable support for conditional processing of PUT, PATCH, and DELETE requests,
   *     based on a strong Entity Tag (ETag) of the feature. Such requests must include an
   *     `If-Match` header, otherwise they will be rejected. A feature will only be changed, if the
   *     feature matches the Etag(s) in the header.
   * @langDe Option zur Aktivierung der Unterstützung für die bedingte Verarbeitung von PUT-, PATCH-
   *     und DELETE-Anfragen, basierend auf einem starken Entity Tag (ETag) des Features. Solche
   *     Anfragen müssen einen `If-Match`-Header enthalten, andernfalls werden sie zurückgewiesen.
   *     Ein Feature wird nur dann geändert, wenn der aktuelle ETag des Features zu den ETag(s) im
   *     Header passt.
   * @default false
   * @since v3.5
   */
  @Nullable
  Boolean getOptimisticLockingETag();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsEtag() {
    return Objects.equals(getOptimisticLockingETag(), true);
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableCrudConfiguration.Builder();
  }
}
