/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Selects the output format of the response. If no value is provided, the standard HTTP
 *     content negotiation rules are applied. That is, the "Accept" header will be used to determine
 *     the format.
 * @langDe Bestimmt das Ausgabeformat der Antwort. Wird kein Wert angegeben, werden die
 *     Standard-HTTP-Inhaltsverhandlungsregeln angewendet. Das heißt, der "Accept"-Header wird zur
 *     Bestimmung des Formats verwendet.
 * @name Collection
 * @endpoints Feature Collection
 */
@Singleton
@AutoBind
public class QueryParameterFCollection extends QueryParameterFSubCollection {

  @Inject
  public QueryParameterFCollection(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "fCollection";
  }

  @Override
  public boolean matchesPath(String definitionPath) {
    return "/collections/{collectionId}".equals(definitionPath);
  }

  @Override
  protected Class<? extends GenericFormatExtension> getFormatClass() {
    return CollectionsFormatExtension.class;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CollectionsConfiguration.class;
  }
}
