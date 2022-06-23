/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.cityjson.domain.CityJsonConfiguration;
import de.ii.ogcapi.features.cityjson.domain.ImmutableCityJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features CityJSON
 * @langEn The module *Features CityJSON* adds support for CityJSON 1.0 and 1.1 as a feature
 *     encoding. Supported are the feature types `Building` and `BuildingPart`.
 * @scopeEn The module requires that the feature provider includes a type `building` that is mapped
 *     to a CityJSON Building feature. Properties of the type `building` are mapped to CityJSON as
 *     follows:
 *     <p><code>
 * - `consistsOfBuildingPart`: The value must be an object with the same properties as `building`.
 *   The object is encoded as a BuildingPart feature of the Building feature.
 * - `address`: The value must be an array of address objects. The following attributes are mapped
 *   to an Address object, all other properties are ignored:
 *   - `multiPoint`: a  MULTI_POINT geometry representing the address location
 *   - `ThoroughfareName`: a string
 *   - `ThoroughfareNumber`: a string
 *   - `LocalityName`: a string
 *   - `PostalCode`: a string
 *   - `AdministrativeArea`: a string
 *   - `CountryName`: a string
 * - `lod1Solid`: a closed MULTI_POLYGON geometry that represents the shell of the building at LoD 1.
 * - `lod2Solid`: a closed MULTI_POLYGON geometry that represents the shell of the building at LoD 2.
 * - `surfaces`: an array of semantic surface objects. The object must have a `surfaceType` and the
 *   values must be one of the following: `CeilingSurface`, `ClosureSurface`, `Door`, `FloorSurface`,
 *   `GroundSurface`, `InteriorWallSurface`, `OuterCeilingSurface`, `OuterFloorSurface`, `RoofSurface`,
 *   `WallSurface`, or `Window`. The object should have a property with a POLYGON or MULTI_POLYGON
 *   geometry that represents the surface geometry.
 * - all other properties: The property will be mapped to a CityJSON attribute, except for `gml_id` properties.
 * </code>
 *     <p>The property of the `building` with the role `ID` will be used as the CityJSON id. Since
 *     the embedded building parts do not have a property with a role `ID`, the building part
 *     feature must have a unique property `id`, which will be used as the id of the CityJSON
 *     building part.
 *     <p>The example includes a sample type definition for the building features in a PostgreSQL
 *     feature provider based on the CityGML profile of the German surveying and mapping
 *     authorities.
 * @langDe Das Modul *Features CityJSON* unterstützt CityJSON 1.0 und 1.1 als Kodierung für
 *     Features. Unterstützt werden die Objektarten `Building` und `BuildingPart`.
 * @scopeDe Das Modul erfordert, dass der Feature-Provider einen Typ `building` enthält, der auf ein
 *     CityJSON-Building-Feature abgebildet wird. Eigenschaften des Typs `building` werden wie folgt
 *     auf CityJSON abgebildet:
 *     <p><code>
 * - `consistsOfBuildingPart`: Der Wert muss ein Objekt mit denselben Eigenschaften wie bei `building` sein.
 *   Das Objekt wird als ein BuildingPart-Feature zu dem Gebäude kodiert.
 * - `address`: Der Wert muss ein Array von Adressobjekten sein. Die folgenden Attribute werden abgebildet
 *   auf ein Address-Objekt abgebildet, alle anderen Eigenschaften werden ignoriert:
 *   - `multiPoint`: eine MULTI_POINT-Geometrie, die den Standort der Adresse darstellt
 *   - `ThoroughfareName`: eine Zeichenkette
 *   - `ThoroughfareNumber`: eine Zeichenkette
 *   - `LocalityName`: eine Zeichenkette
 *   - `PostalCode`: eine Zeichenkette
 *   - `AdministrativeArea`: eine Zeichenkette
 *   - `CountryName`: eine Zeichenkette
 * - `lod1Solid`: eine geschlossene MULTI_POLYGON-Geometrie, die die Hülle des Gebäudes bei LoD 1 darstellt.
 * - `lod2Solid`: eine geschlossene MULTI_POLYGON-Geometrie, die die Hülle des Gebäudes bei LoD 2 darstellt.
 * - `surfaces`: ein Array von semantischen Oberflächenobjekten. Das Objekt muss einen `surfaceType` haben und die
 *   Werte müssen einer der folgenden sein: `CeilingSurface`, `ClosureSurface`, `Door`, `FloorSurface`,
 *   `GroundSurface`, `InteriorWallSurface`, `OuterCeilingSurface`, `OuterFloorSurface`, `RoofSurface`,
 *   `WallSurface`, oder `Window`. Das Objekt sollte eine Eigenschaft mit einer POLYGON oder MULTI_POLYGON
 *   Geometrie, die die Flächengeometrie darstellt.
 * - alle anderen Eigenschaften: Die Eigenschaft wird auf ein CityJSON-Attribut abgebildet, mit Ausnahme der Eigenschaft "gml_id".
 * </code>
 *     <p>Die Eigenschaft des `building`-Typs mit der Rolle `ID` wird als CityJSON-ID verwendet. Da
 *     die eingebetteten Gebäudeteile keine Eigenschaft mit der Rolle `ID` haben, muss das
 *     Gebäudeteilmerkmal eine eindeutige Eigenschaft `id` haben, die als Id des
 *     CityJSON-Gebäudeteils verwendet wird.
 *     <p>Das Beispiel enthält eine Typdefinition für Gebäudeobjekte in einem
 *     PostgreSQL-Feature-Provider auf Grundlage des CityGML-Profils der deutschen
 *     Vermessungsverwaltung.
 * @example {@link de.ii.ogcapi.features.cityjson.domain.CityJsonConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.cityjson.domain.ImmutableCityJsonConfiguration}
 */
@Singleton
@AutoBind
public class CityJsonBuildingBlock implements ApiBuildingBlock {

  @Inject
  CityJsonBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCityJsonConfiguration.Builder()
        .enabled(false)
        .textSequences(false)
        .version(CityJsonConfiguration.Version.V11)
        .build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, ValidationResult.MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == ValidationResult.MODE.NONE) {
      return ValidationResult.of();
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    OgcApiDataV2 apiData = api.getData();

    // check that text sequences are not enabled for version 1.0
    apiData
        .getCollections()
        .forEach(
            (key, value) ->
                value
                    .getExtension(CityJsonConfiguration.class)
                    .ifPresent(
                        config -> {
                          if (config.isEnabled()
                              && config.getTextSequences().orElse(false)
                              && config
                                  .getVersion()
                                  .filter(v -> v.equals(CityJsonConfiguration.Version.V10))
                                  .isPresent()) {
                            builder.addErrors(
                                "CityJSON Text Sequences can only be enabled for CityJSON 1.1 or later, not for CityJSON 1.0.");
                          }
                        }));

    return builder.build();
  }
}
