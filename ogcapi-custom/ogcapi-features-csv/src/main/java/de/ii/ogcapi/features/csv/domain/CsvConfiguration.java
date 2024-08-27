/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.csv.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.FeatureFormatConfiguration;
import de.ii.ogcapi.features.core.domain.SfFlatConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import org.immutables.value.Value;

/**
 * @buildingBlock CSV
 * @examplesEn The following configuration enables CSV as a feature encoding, where all objects and
 *     arrays are flattened with an underscore as a separator and two properties per array property.
 *     <p><code>
 * ```yaml
 * - buildingBlock: CSV
 *   enabled: true
 *   transformations:
 *     '*':
 *       flatten: '_'
 *   maxMultiplicity: 2
 * ```
 *     <p>For a feature with the following "properties" member in GeoJSON:
 *     <p><code>
 * ```yaml
 * {
 *   "att1": "foo",
 *   "att2": [ "bar1", "bar2", "bar3" ]
 *   "att3": {
 *     "value": 123,
 *     "values": [ 456, 789, 0 ]
 *   }
 * }
 * ```
 *     </code>
 *     <p>The resulting CSV file would be:
 *     <p><code>
 * ```csv
 * att1,att2_1,att2_2,att3_value,att3_values_1,att3_values_2
 * foo,bar1,bar2,123,456,789
 * ```
 *     </code>
 * @examplesDe Die folgende Konfiguration aktiviert CSV als Feature-Kodierung, bei der alle Objekte und Arrays mit einem Unterstrich als Trennzeichen und zwei Eigenschaften pro Array-Eigenschaft abgeflacht werden.
 *     <p><code>
 * ```yaml
 * - buildingBlock: CSV
 *   enabled: true
 *   transformations:
 *     '*':
 *       flatten: '_'
 *   maxMultiplicity: 2
 * ```
 *     <p>Für ein Feature mit den folgenden GeoJSON-"properties":
 *     <p><code>
 * ```yaml
 * {
 *   "att1": "foo",
 *   "att2": [ "bar1", "bar2", "bar3" ]
 *   "att3": {
 *     "value": 123,
 *     "values": [ 456, 789, 0 ]
 *   }
 * }
 * ```
 * </code>
 *     <p>Die resultierende CSV-Datei würde wie folgt aussehen:
 *     <p><code>
 * ```csv
 * att1,att2_1,att2_2,att3_value,att3_values_1,att3_values_2
 * foo,bar1,bar2,123,456,789
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "CSV")
@JsonDeserialize(builder = ImmutableCsvConfiguration.Builder.class)
public interface CsvConfiguration extends SfFlatConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableCsvConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCsvConfiguration.Builder builder =
        ((ImmutableCsvConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this)
            .transformations(
                SfFlatConfiguration.super
                    .mergeInto((PropertyTransformations) source)
                    .getTransformations())
            .defaultProfiles(
                this.getDefaultProfiles().isEmpty()
                    ? ((FeatureFormatConfiguration) source).getDefaultProfiles()
                    : this.getDefaultProfiles());

    return builder.build();
  }
}
