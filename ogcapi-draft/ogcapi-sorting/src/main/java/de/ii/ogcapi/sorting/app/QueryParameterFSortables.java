/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.QueryParameterFSubCollection;
import de.ii.ogcapi.sorting.domain.SortingConfiguration;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat;
import de.ii.ogcapi.foundation.domain.*;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterFSortables extends QueryParameterFSubCollection {

  @Inject
  public QueryParameterFSortables(ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "fSortables";
  }

  @Override
  protected boolean matchesPath(String definitionPath) {
    return definitionPath.equals("/collections/{collectionId}/sortables");
  }

  @Override
  protected Class<? extends FormatExtension> getFormatClass() {
    return CollectionPropertiesFormat.class;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SortingConfiguration.class;
  }

}