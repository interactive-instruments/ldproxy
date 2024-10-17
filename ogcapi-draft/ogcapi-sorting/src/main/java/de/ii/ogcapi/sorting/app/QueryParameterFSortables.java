/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.sorting.domain.SortingConfiguration;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title f
 * @endpoints Sortables
 * @langEn Select the output format of the response. If no value is provided, the standard HTTP
 *     rules apply, i.e., the "Accept" header will be used to determine the format.
 * @langDe Wählt das Ausgabeformat der Antwort. Wenn kein Wert angegeben wird, gelten die
 *     Standard-HTTP Regeln, d.h. der "Accept"-Header wird zur Bestimmung des Formats verwendet.
 */
@Singleton
@AutoBind
public class QueryParameterFSortables extends QueryParameterF {

  @Inject
  public QueryParameterFSortables(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "fSortables";
  }

  @Override
  public boolean matchesPath(String definitionPath) {
    return "/collections/{collectionId}/sortables".equals(definitionPath);
  }

  @Override
  protected Class<? extends FormatExtension> getFormatClass() {
    return CollectionPropertiesFormat.class;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SortingConfiguration.class;
  }

  @Override
  public Optional<SpecificationMaturity> getSpecificationMaturity() {
    return Optional.of(SpecificationMaturity.DRAFT_LDPROXY);
  }
}
