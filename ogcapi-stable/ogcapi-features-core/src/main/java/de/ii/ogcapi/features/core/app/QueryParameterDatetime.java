/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.AbstractQueryParameterDatetime;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title datetime
 * @endpoints Features
 * @langEn Select only features that have a primary instant or interval that intersects the provided
 *     instant or interval.
 * @langDe Es werden nur Features ausgewählt, deren primäre zeitliche Eigenschaft den angegebenen
 *     Wert (Zeitstempel, Datum oder Intervall) schneidet.
 */
@Singleton
@AutoBind
public class QueryParameterDatetime extends AbstractQueryParameterDatetime {

  @Inject
  QueryParameterDatetime(SchemaValidator schemaValidator) {
    super(schemaValidator);
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && definitionPath.equals("/collections/{collectionId}/items"));
  }
}
