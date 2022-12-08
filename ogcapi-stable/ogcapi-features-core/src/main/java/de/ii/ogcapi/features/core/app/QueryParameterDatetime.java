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
 * @langEn Either a date-time or an interval. Date and time expressions adhere to RFC 3339.
 *     <p>Intervals may be bounded or half-bounded (double-dots at start or end).
 *     <p>Examples:
 *     <p>
 *     <p>* A date-time: "2018-02-12T23:20:50Z"
 *     <p>* A bounded interval: "2018-02-12T00:00:00Z/2018-03-18T12:31:12Z"
 *     <p>* Half-bounded intervals: "2018-02-12T00:00:00Z/.." or "../2018-03-18T12:31:12Z"
 *     <p>
 *     <p>Only features that have a temporal property that intersects the value of `datetime` are
 *     selected.
 * @langDe Entweder ein Datum/Zeitpunkt oder ein Intervall. Datums- und Zeitausdrücke entsprechen
 *     RFC 3339.
 *     <p>Intervalle können begrenzt oder halb-begrenzt sein (Doppelpunkte am Anfang oder Ende).
 *     <p>Beispiele:
 *     <p>
 *     <p>* Ein Datum-Zeitpunkt: "2018-02-12T23:20:50Z"
 *     <p>* Ein begrenztes Intervall: "2018-02-12T00:00:00Z/2018-03-18T12:31:12Z"
 *     <p>* Half-bounded intervals: "2018-02-12T00:00:00Z/.." oder "../2018-03-18T12:31:12Z"
 *     <p>
 *     <p>Es werden nur Merkmale ausgewählt, die eine zeitliche Eigenschaft haben, die den Wert von
 *     `datetime` überschneidet.
 * @name datetime
 * @endpoints Features
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
                && "/collections/{collectionId}/items".equals(definitionPath));
  }
}
