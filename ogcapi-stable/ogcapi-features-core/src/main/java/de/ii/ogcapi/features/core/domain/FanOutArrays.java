/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

/**
 * Replaces any array properties that were created by the flatten transformation with multiple
 * properties with the property names that are used in the instances. For example, if the separator
 * is an underscore ("_") and three properties are supported for each array property, then the
 * property with name "foo[]" is replaced by three properties "foo_1", "foo_2" and "foo_3".
 */
public class FanOutArrays implements SchemaVisitorTopDown<FeatureSchema, List<FeatureSchema>> {

  private final String separator;
  private final int maxMultiplicity;

  public FanOutArrays(String separator, int maxMultiplicity) {
    this.separator = separator;
    this.maxMultiplicity = maxMultiplicity;
  }

  @Override
  public List<FeatureSchema> visit(
      FeatureSchema schema,
      List<FeatureSchema> parents,
      List<List<FeatureSchema>> visitedProperties) {

    if (parents.isEmpty()) {
      Map<String, FeatureSchema> propertyMap =
          visitedProperties.stream()
              .flatMap(Collection::stream)
              .map(property -> new SimpleEntry<>(property.getName(), property))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));
      return ImmutableList.of(
          new ImmutableFeatureSchema.Builder().from(schema).propertyMap(propertyMap).build());
    }

    String propertyName = schema.getName();
    if (propertyName.contains("[]")) {
      int arrayCount = StringUtils.countMatches(propertyName, "[]");
      int[] indicees = new int[arrayCount];
      Arrays.fill(indicees, 1);
      return fanOut(schema, indicees, 0);
    }

    return ImmutableList.of(schema);
  }

  private FeatureSchema replaceArrays(FeatureSchema property, int[] indicees) {
    String name = property.getName();
    for (int indicee : indicees) {
      name = name.replaceFirst("\\[\\]", String.format("%s%d", separator, indicee));
    }
    return new ImmutableFeatureSchema.Builder().from(property).name(name).build();
  }

  private List<FeatureSchema> fanOut(FeatureSchema property, int[] indicees, int array) {
    if (array == indicees.length) return ImmutableList.of(replaceArrays(property, indicees));
    else {
      ImmutableList.Builder<FeatureSchema> builder = new ImmutableList.Builder<FeatureSchema>();
      for (indicees[array] = 1; indicees[array] <= maxMultiplicity; indicees[array]++) {
        builder.addAll(fanOut(property, indicees, array + 1));
      }
      return builder.build();
    }
  }
}
