/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration.PathSeparator;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock SORTING
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: SORTING
 *   enabled: true
 *   included:
 *   - name
 *   - function
 *   - height
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "SORTING")
@JsonDeserialize(builder = ImmutableSortingConfiguration.Builder.class)
public interface SortingConfiguration extends ExtensionConfiguration {

  /**
   * @langEn Controls which of the attributes in queries can be used for sorting data. Only direct
   *     attributes of the data types `STRING`, `DATE`, `DATETIME`, `INTEGER` and `FLOAT` are
   *     eligible as sortables (that is, no attributes from arrays or embedded objects) unless
   *     `isSortable` is set to `false` for the property. The special value `*` includes all
   *     eligible properties as sortables. By default, no property is sortable.
   * @langDe Ersetzt durch `included`. Steuert, welche der Attribute in Queries für die Sortierung
   *     von Daten verwendet werden können. Als Sortables kommen nur direkte Attribute (keine
   *     Attribute aus Arrays oder eingebetteten Objekten) der Datentypen `STRING`, `DATE`,
   *     `DATETIME`, `INTEGER` und `FLOAT` in Frage, es sei denn `isSortable` ist für die
   *     Eigenschaft auf `false` gesetzt. Der spezielle Wert `*` schließt alle infrage kommenden
   *     Eigenschaften als sortierbar ein. Standardmäßig ist keine Eigenschaft sortierbar.
   * @default []
   * @since v3.4
   */
  List<String> getIncluded();

  /**
   * @langEn The list of properties that would be sortables based on `included`, but which should
   *     not be sortables.
   * @langDe Die Liste der Eigenschaften, die aufgrund von `included` sortierbar wären, aber nicht
   *     sortierbar sein sollen.
   * @default []
   * @since v3.4
   */
  List<String> getExcluded();

  /**
   * @langEn The character that is used as the path separator in case of object-valued properties.
   *     Either `DOT` or `UNDERSCORE`.
   * @langDe Das Zeichen, das im Falle von objektwertigen Eigenschaften als Pfadseparator verwendet
   *     wird. Entweder `DOT` (Punkt) oder `UNDERSCORE` (Unterstrich).
   * @default DOT
   * @since v3.4
   */
  @Nullable
  PathSeparator getPathSeparator();

  default Map<String, FeatureSchema> getSortables(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      FeatureSchema schema,
      FeaturesCoreProviders providers) {
    return getSortablesSchema(apiData, collectionData, schema, providers)
        .getAllNestedProperties()
        .stream()
        .filter(FeatureSchema::sortable)
        .map(
            subschema ->
                new SimpleImmutableEntry<>(
                    subschema.getFullPathAsString(getPathSeparator().toString()), subschema))
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, (first, second) -> second));
  }

  default FeatureSchema getSortablesSchema(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      FeatureSchema schema,
      FeaturesCoreProviders providers) {
    FeatureQueries featureQueries =
        providers.getFeatureProviderOrThrow(apiData, collectionData, FeatureProvider2::queries);

    return featureQueries.getSortablesSchema(
        schema, getIncluded(), getExcluded(), getPathSeparator().toString());
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableSortingConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableSortingConfiguration.Builder()
        .from(source)
        .from(this)
        .included(
            Stream.concat(
                    ((SortingConfiguration) source).getIncluded().stream(), getIncluded().stream())
                .distinct()
                .collect(Collectors.toList()))
        .excluded(
            Stream.concat(
                    ((SortingConfiguration) source).getExcluded().stream(), getExcluded().stream())
                .distinct()
                .collect(Collectors.toList()))
        .build();
  }
}
