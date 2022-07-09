/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGmlConfiguration.Builder.class)
public interface GmlConfiguration extends ExtensionConfiguration, PropertyTransformations {

  // TODO add documentation

  enum Conformance {
    NONE,
    GMLSF0,
    GMLSF2
  }

  @Nullable
  Conformance getConformance();

  Map<String, String> getApplicationNamespaces();

  @Nullable
  String getDefaultNamespace();

  Map<String, String> getSchemaLocations();

  Map<String, String> getObjectTypeNamespaces();

  Map<String, VariableName> getVariableObjectElementNames();

  @Nullable
  String getFeatureCollectionElementName();

  @Nullable
  String getFeatureMemberElementName();

  @Nullable
  Boolean getSupportsStandardResponseParameters();

  List<String> getXmlAttributes();

  @Nullable
  String getGmlIdPrefix();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableGmlConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    // TODO handle all other properties
    return new ImmutableGmlConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(
            PropertyTransformations.super
                .mergeInto((PropertyTransformations) source)
                .getTransformations())
        .build();
  }
}
