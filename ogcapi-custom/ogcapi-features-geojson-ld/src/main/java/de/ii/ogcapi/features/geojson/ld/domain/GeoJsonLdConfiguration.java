/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock GEO_JSON_LD
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: GEO_JSON_LD
 *   enabled: true
 *   context: '{{serviceUrl}}/collections/{{collectionId}}/context'
 *   types:
 *   - geojson:Feature
 *   - sosa:Observation
 *   idTemplate: '{{serviceUrl}}/collections/{{collectionId}}/items/{{featureId}}'
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableGeoJsonLdConfiguration.Builder.class)
public interface GeoJsonLdConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn File name of the JSON-LD context document in the folder
   *     `api-resources/json-ld-contexts/{apiId}`.
   * @langDe Dateiname des JSON-LD-Context-Dokuments im Verzeichnis
   *     `api-resources/json-ld-contexts/{apiId}`.
   * @default null
   */
  @Nullable
  String getContextFileName();

  /**
   * @langEn URI of the JSON-LD context document. The value should either be an external URI or
   *     `{{serviceUrl}}/collections/{{collectionId}}/context` for contexts provided by the API (see
   *     below for details). The template may contain `{{serviceUrl}}` (substituted with the API
   *     landing page URI) and `{{collectionId}}` (substituted with the collection id).
   * @langDe Die URI des JSON-LD-Context-Dokuments. Dabei wird `{{serviceUrl}}` durch die
   *     Landing-Page-URI der API und `{{collectionId}}` durch die Collection-ID ersetzt. Sofern der
   *     Context nicht extern liegt, sollte der Wert
   *     "{{serviceUrl}}/collections/{{collectionId}}/context" sein.
   * @default null
   */
  @Nullable
  String getContext();

  /**
   * @langEn Value of `@type` that is added to every feature.
   * @langDe Der Wert von "@type" bei den Features der Collection. Dabei wird `{{type}}` durch den
   *     Wert der Property mit `role: TYPE` ersetzt.
   * @default [ "geojson:Feature" ]
   */
  List<String> getTypes();

  /**
   * @langEn Value of `@id` that is added to every feature. The template may contain
   *     `{{serviceUrl}}` (substituted with the API landing page URI), `{{collectionId}}`
   *     (substituted with the collection id) and `{{featureId}}` (substituted with the feature id).
   * @langDe Der Wert von "@id" bei den Features der Collection. Dabei wird `{{serviceUrl}}` durch
   *     die Landing-Page-URI der API, `{{collectionId}}` durch die Collection-ID und
   *     `{{featureId}}` durch den Wert von "id" ersetzt.
   * @default
   */
  Optional<String> getIdTemplate();

  @Override
  default Builder getBuilder() {
    return new ImmutableGeoJsonLdConfiguration.Builder();
  }
}
