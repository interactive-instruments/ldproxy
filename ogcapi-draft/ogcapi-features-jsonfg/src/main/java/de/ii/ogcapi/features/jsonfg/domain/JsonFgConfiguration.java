/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock JSON_FG
 * @examplesEn Example of the information in the configuration file for the entire API (from the API
 *     for [Topographic Data in Daraa, Syria](https://demo.ldproxy.net/daraa)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: JSON_FG
 *   enabled: true
 *   featureType:
 *   - nas:{{type}}
 * ```
 *     </code>
 *     <p>Additional information per feature collection with an attribute `F_CODE` (for which `role:
 *     TYPE` was set in the provider configuration) to set the object type:
 *     <p><code>
 * ```yaml
 * - buildingBlock: JSON_FG
 *   featureType:
 *   - nas:{{type}}
 * ```
 *     </code>
 *     <p>This outputs the object type as follows for a value of "GB075" in the 'F_CODE' attribut:
 *     <p><code>
 * ```json
 * {
 *   "type": "Feature",
 *   "id": 1,
 *   "featureType": "nas:GB075",
 *   ...
 * }
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API (aus der API
 *     für [Topographische Daten in Daraa, Syrien](https://demo.ldproxy.net/daraa)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: JSON_FG
 *   enabled: true
 *   featureType:
 *   - nas:{{type}}
 * ```
 *     </code>
 *     <p>Ergänzende Angaben pro Feature Collection mit einem Attribut `F_CODE` (für das in der
 *     Provider-Konfiguration `role: TYPE` gesetzt wurde), um die Objektart zu setzen:
 *     <p><code>
 * ```yaml
 * - buildingBlock: JSON_FG
 *   featureType:
 *   - nas:{{type}}
 * ```
 *     </code>
 *     <p>Hierdurch wird bei einem Wert von "GB075" im Attribut `F_CODE` die Objektart wie folgt
 *     ausgegeben:
 *     <p><code>
 * ```json
 * {
 *   "type": "Feature",
 *   "id": 1,
 *   "featureType": "nas:GB075",
 *   ...
 * }
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = Builder.class)
public interface JsonFgConfiguration extends ExtensionConfiguration, PropertyTransformations {

  enum OPTION {
    describedby,
    featureType,
    time,
    place,
    coordRefSys,
    links,
    geometryDimension
  }

  /**
   * @langEn Enables the output of links to JSON Schema documents to the JSON instant, e.g. for
   *     validation purposes.
   * @langDe Aktiviert die Ausgabe von Links auf JSON-Schema-Dokumente zu der JSON-Instant, z.B. zur
   *     Validierung
   * @default true
   */
  @Nullable
  Boolean getDescribedby();

  /**
   * @langEn Activates the output of "coordRefSys" for features
   * @langDe Aktiviert die Ausgabe von "coordRefSys" bei Features
   * @default true
   */
  @Nullable
  Boolean getCoordRefSys();

  /**
   * @langEn Activates support for the "compatibility=geojson" media type parameter
   * @langDe Aktiviert die Unterstützung für den "compatibility=geojson" Media-Type-Parameter
   * @default true
   */
  @Nullable
  Boolean getGeojsonCompatibility();

  /**
   * @langEn Activates the output of "featureType" with the specified values for features. If an
   *     object type is specified, then a string is output, otherwise an array of strings.
   * @langDe Aktiviert die Ausgabe von "featureType" mit den angegebenen Werten bei Features. Ist
   *     eine Objektart angegeben, dann wird ein String ausgegeben, ansonsten ein Array von Strings.
   * @default []
   */
  @Nullable
  List<String> getFeatureType();

  /**
   * @langEn Adds the specified links to the `links` array of features. All values of the array must
   *     be a valid link object with `href` and `rel`.
   * @langDe Ergänzt den "links"-Array von Features um die angegebenen Links. Alle Werte des Arrays
   *     müssen ein gültiges Link-Objekt mit `href` und `rel` sein.
   * @default []
   */
  @Nullable
  List<Link> getLinks();

  /**
   * @langEn The option allows selected JSON-FG extensions to be included in the GeoJSON encoding as
   *     well. Allowed values are: `describedby`, `featureType`, `time`, `place`, `coordRefSys`,
   *     `links`.
   * @langDe Die Option ermöglicht, dass ausgewählte JSON-FG-Erweiterungen auch im GeoJSON-Encoding
   *     berücksichtigt werden. Erlaubte Werte sind: `describedby`, `featureType`, `time`, `place`,
   *     `coordRefSys`, `links``
   * @default []
   */
  List<OPTION> getIncludeInGeoJson();

  @Override
  default Builder getBuilder() {
    return new ImmutableJsonFgConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableJsonFgConfiguration.Builder builder =
        new ImmutableJsonFgConfiguration.Builder().from(source).from(this);

    ImmutableJsonFgConfiguration src = (ImmutableJsonFgConfiguration) source;

    if (Objects.nonNull(getFeatureType())) builder.featureType(getFeatureType());
    else if (Objects.nonNull(src.getFeatureType())) builder.featureType(src.getFeatureType());

    return builder.build();
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}
}
