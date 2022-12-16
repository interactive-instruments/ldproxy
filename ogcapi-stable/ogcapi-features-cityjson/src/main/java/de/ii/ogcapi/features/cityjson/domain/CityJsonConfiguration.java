/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: CITY_JSON
 *   enabled: true
 *   textSequences: false
 *   version: V11
 * ```
 * </code>
 *     <p>Feature Provider:
 *     <p><code>
 * ```yaml
 * types:
 *   building:
 *     label: Gebäude
 *     sourcePath: /building{filter=fk_buildingpart_parent IS NULL}
 *     type: OBJECT
 *     objectType: Building
 *     properties:
 *       id:
 *         label: Objekt-ID
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *       gml_id:
 *         label: ALKIS-ID
 *         sourcePath: gml_id
 *         type: STRING
 *       name:
 *         label: Name
 *         description: 'Nur bei Gebäudeeigennamen gesetzt.'
 *         sourcePath: feature_name
 *         type: STRING
 *       bbox:
 *         label: Bounding Box
 *         sourcePath: envelope
 *         type: GEOMETRY
 *         geometryType: POLYGON
 *         role: PRIMARY_GEOMETRY
 *       lod12GroundSurface:
 *         label: Grundfläche
 *         sourcePath: lod12groundsurface
 *         type: GEOMETRY
 *         geometryType: MULTI_POLYGON
 *       measuredHeight:
 *         label: Gebäudehöhe [m]
 *         description: 'Höhe des Gebäudes aus der Differenz in Metern zwischen dem höchsten Bezugspunkt und dem tiefsten Bezugspunkt des Gebäudes.'
 *         sourcePath: measuredheight
 *         type: FLOAT
 *       storeysAboveGround:
 *         label: Geschosse
 *         sourcePath: storeysaboveground
 *         type: INTEGER
 *       roofType:
 *         label: Dachform
 *         description: 'Die Bedeutung der einzelnen Werte ergibt aus der Codelist <a href="https://repository.gdi-de.org/schemas/adv/citygml/Codelisten/RoofTypeTypeAdV.xml" target="_blank">RoofTypeTypeAdV.xml</a>.'
 *         sourcePath: rooftype
 *         type: STRING
 *         constraints:
 *           codelist: RoofType
 *       creationDate:
 *         label: Erzeugungsdatum
 *         sourcePath: creation
 *         type: DATE
 *         role: PRIMARY_INSTANT
 *       function:
 *         label: Gebäudefunktion
 *         description: 'Die Bedeutung der einzelnen Werte ergibt aus der Codelist <a href="https://repository.gdi-de.org/schemas/adv/citygml/Codelisten/BuildingFunctionTypeAdV.xml" target="_blank">BuildingFunctionTypeAdV.xml</a>.'
 *         sourcePath: function
 *         type: STRING
 *         constraints:
 *           codelist: BuildingFunctionType
 *       externalReferences:
 *         label: Fachdatenverbindungen
 *         sourcePath: '[id=fk_feature]extref_building{sortKey=name}'
 *         type: OBJECT_ARRAY
 *         properties:
 *           name:
 *             label: Bezeichnung
 *             sourcePath: name
 *             type: STRING
 *           informationSystem:
 *             label: Informationssystem
 *             sourcePath: informationsystem
 *             type: STRING
 *       bezugspunktDach:
 *         label: Bezugspunkt Dach
 *         sourcePath: '[id=fk_feature]att_string_building{filter=name=''BezugspunktDach''}{sortKey=name}/value'
 *         type: STRING
 *         constraints:
 *           codelist: BezugspunktDach
 *       datenquelleBodenhoehe:
 *         label: Datenquelle Bodenhöhe
 *         sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleBodenhoehe''}{sortKey=name}/value'
 *         type: STRING
 *         constraints:
 *           codelist: DatenquelleBodenhoehe
 *       datenquelleDachhoehe:
 *         label: Datenquelle Dachhöhe
 *         sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleDachhoehe''}{sortKey=name}/value'
 *         type: STRING
 *         constraints:
 *           codelist: DatenquelleDachhoehe
 *       datenquelleLage:
 *         label: Datenquelle Lage
 *         sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleLage''}{sortKey=name}/value'
 *         type: STRING
 *         constraints:
 *           codelist: DatenquelleLage
 *       gemeindeschluessel:
 *         label: Gemeindeschlüssel
 *         sourcePath: '[id=fk_feature]att_string_building{filter=name=''Gemeindeschluessel''}{sortKey=name}/value'
 *         type: STRING
 *       address:
 *         label: Adresse
 *         sourcePath: '[id=fk_feature]address{sortKey=id}'
 *         type: OBJECT_ARRAY
 *         properties:
 *           id:
 *             label: Objekt-ID
 *             sourcePath: id
 *             type: INTEGER
 *             transformations:
 *               remove: ALWAYS
 *           multiPoint:
 *             sourcePath: multipoint
 *             type: GEOMETRY
 *             geometryType: MULTI_POINT
 *             forcePolygonCCW: false
 *           ThoroughfareName:
 *             label: Straße
 *             sourcePath: street
 *             type: STRING
 *           ThoroughfareNumber:
 *             label: Hausnummer
 *             sourcePath: code
 *             type: STRING
 *           PostalCode:
 *             label: Postleitzahl
 *             sourcePath: postalcode
 *             type: STRING
 *           LocalityName:
 *             label: Stadt
 *             sourcePath: city
 *             type: STRING
 *           AdministrativeArea:
 *             label: Verwaltungsbezirk
 *             sourcePath: administrativearea
 *             type: STRING
 *           CountryName:
 *             label: Land
 *             sourcePath: country
 *             type: STRING
 *       lod1Solid:
 *         label: Geometrie (LoD 1)
 *         sourcePath: lod1solid
 *         type: GEOMETRY
 *         geometryType: MULTI_POLYGON
 *         forcePolygonCCW: false
 *       lod1TerrainIntersection:
 *         sourcePath: lod1terrainintersection
 *         type: GEOMETRY
 *         geometryType: MULTI_LINE_STRING
 *         forcePolygonCCW: false
 *       lod2Solid:
 *         label: Geometrie (LoD 2)
 *         sourcePath: lod2solid
 *         type: GEOMETRY
 *         geometryType: MULTI_POLYGON
 *         forcePolygonCCW: false
 *       surfaces:
 *         label: Begrenzungsflächen (LoD 2)
 *         sourcePath: '[id=fk_feature]building_surface{sortKey=gml_id}'
 *         type: OBJECT_ARRAY
 *         properties:
 *           gml_id:
 *             label: ID
 *             sourcePath: gml_id
 *             type: STRING
 *           creationDate:
 *             label: Erzeugungsdatum
 *             sourcePath: creation
 *             type: DATE
 *             transformations:
 *               dateFormat: 'yyyy-MM-dd'
 *           surfaceType:
 *             label: Flächentyp
 *             sourcePath: surface_type
 *             type: STRING
 *           lod2MultiSurface:
 *             label: Geometrie (LoD 2)
 *             sourcePath: geom
 *             type: GEOMETRY
 *             geometryType: MULTI_POLYGON
 *             forcePolygonCCW: false
 *       lod2TerrainIntersection:
 *         sourcePath: lod2terrainintersection
 *         type: GEOMETRY
 *         geometryType: MULTI_LINE_STRING
 *         forcePolygonCCW: false
 *       fk_buildingpart_parent:
 *         label: Gebäude-Objekt-ID
 *         sourcePath: fk_buildingpart_parent
 *         type: INTEGER
 *       consistsOfBuildingPart:
 *         label: Gebäudeteile
 *         sourcePath: '[id=fk_buildingpart_parent]building'
 *         type: OBJECT_ARRAY
 *         objectType: BuildingPart
 *         properties:
 *           id:
 *             label: Objekt-ID
 *             sourcePath: id
 *             type: INTEGER
 *           gml_id:
 *             label: ALKIS-ID
 *             sourcePath: gml_id
 *             type: STRING
 *           name:
 *             label: Name
 *             sourcePath: feature_name
 *             type: STRING
 *           lod12GroundSurface:
 *             sourcePath: lod12groundsurface
 *             type: GEOMETRY
 *             geometryType: MULTI_POLYGON
 *           measuredHeight:
 *             label: Gebäudehöhe [m]
 *             sourcePath: measuredheight
 *             type: FLOAT
 *           storeysAboveGround:
 *             label: Geschosse
 *             sourcePath: storeysaboveground
 *             type: INTEGER
 *           roofType:
 *             label: Dachform
 *             description: 'Die Bedeutung der einzelnen Werte ergibt aus der Codelist <a href="https://repository.gdi-de.org/schemas/adv/citygml/Codelisten/RoofTypeTypeAdV.xml" target="_blank">RoofTypeTypeAdV.xml</a>.'
 *             sourcePath: rooftype
 *             type: STRING
 *             constraints:
 *               codelist: RoofType
 *           creationDate:
 *             label: Erzeugungsdatum
 *             sourcePath: creation
 *             type: DATE
 *           function:
 *             label: Gebäudefunktion
 *             sourcePath: function
 *             type: STRING
 *             constraints:
 *               codelist: BuildingFunctionType
 *           externalReferences:
 *             label: Fachdatenverbindungen
 *             sourcePath: '[id=fk_feature]extref_building{sortKey=name}'
 *             type: OBJECT_ARRAY
 *             properties:
 *               name:
 *                 label: Bezeichnung
 *                 sourcePath: name
 *                 type: STRING
 *               informationSystem:
 *                 label: Informationssystem
 *                 sourcePath: informationsystem
 *                 type: STRING
 *           bezugspunktDach:
 *             label: Bezugspunkt Dach
 *             sourcePath: '[id=fk_feature]att_string_building{filter=name=''BezugspunktDach''}{sortKey=name}/value'
 *             type: STRING
 *             constraints:
 *               codelist: BezugspunktDach
 *           datenquelleBodenhoehe:
 *             label: Datenquelle Bodenhöhe
 *             sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleBodenhoehe''}{sortKey=name}/value'
 *             type: STRING
 *             constraints:
 *               codelist: DatenquelleBodenhoehe
 *           datenquelleDachhoehe:
 *             label: Datenquelle Dachhöhe
 *             sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleDachhoehe''}{sortKey=name}/value'
 *             type: STRING
 *             constraints:
 *               codelist: DatenquelleDachhoehe
 *           datenquelleLage:
 *             label: Datenquelle Lage
 *             sourcePath: '[id=fk_feature]att_string_building{filter=name=''DatenquelleLage''}{sortKey=name}/value'
 *             type: STRING
 *             constraints:
 *               codelist: DatenquelleLage
 *           address:
 *             label: Adresse
 *             sourcePath: '[id=fk_feature]address{sortKey=id}'
 *             type: OBJECT_ARRAY
 *             properties:
 *               id:
 *                 label: Objekt-ID
 *                 sourcePath: id
 *                 type: INTEGER
 *                 transformations:
 *                   remove: ALWAYS
 *               multiPoint:
 *                 sourcePath: multipoint
 *                 type: GEOMETRY
 *                 geometryType: MULTI_POINT
 *                 forcePolygonCCW: false
 *               ThoroughfareName:
 *                 label: Straße
 *                 sourcePath: street
 *                 type: STRING
 *               ThoroughfareNumber:
 *                 label: Hausnummer
 *                 sourcePath: code
 *                 type: STRING
 *               PostalCode:
 *                 label: Postleitzahl
 *                 sourcePath: postalcode
 *                 type: STRING
 *               LocalityName:
 *                 label: Stadt
 *                 sourcePath: city
 *                 type: STRING
 *               AdministrativeArea:
 *                 label: Verwaltungsbezirk
 *                 sourcePath: administrativearea
 *                 type: STRING
 *               CountryName:
 *                 label: Land
 *                 sourcePath: country
 *                 type: STRING
 *           lod1Solid:
 *             label: Geometrie (LoD 1)
 *             sourcePath: lod1solid
 *             type: GEOMETRY
 *             geometryType: MULTI_POLYGON
 *             forcePolygonCCW: false
 *           lod1TerrainIntersection:
 *             sourcePath: lod1terrainintersection
 *             type: GEOMETRY
 *             geometryType: MULTI_LINE_STRING
 *             forcePolygonCCW: false
 *           lod2Solid:
 *             label: Geometrie (LoD 2)
 *             sourcePath: lod2solid
 *             type: GEOMETRY
 *             geometryType: MULTI_POLYGON
 *             forcePolygonCCW: false
 *           surfaces:
 *             label: Begrenzungsflächen (LoD 2)
 *             sourcePath: '[id=fk_feature]building_surface{sortKey=gml_id}'
 *             type: OBJECT_ARRAY
 *             properties:
 *               gml_id:
 *                 label: ID
 *                 sourcePath: gml_id
 *                 type: STRING
 *               creationDate:
 *                 label: Erzeugungsdatum
 *                 sourcePath: creation
 *                 type: DATE
 *                 transformations:
 *                   dateFormat: 'yyyy-MM-dd'
 *               surfaceType:
 *                 label: Flächentyp
 *                 sourcePath: surface_type
 *                 type: STRING
 *               lod2MultiSurface:
 *                 label: Geometrie (LoD 2)
 *                 sourcePath: geom
 *                 type: GEOMETRY
 *                 geometryType: MULTI_POLYGON
 *                 forcePolygonCCW: false
 *           lod2TerrainIntersection:
 *             sourcePath: lod2terrainintersection
 *             type: GEOMETRY
 *             geometryType: MULTI_LINE_STRING
 *             forcePolygonCCW: false
 *           fk_buildingpart_parent:
 *             label: Gebäude-Objekt-ID
 *             sourcePath: fk_buildingpart_parent
 *             type: INTEGER
 *             transformations:
 *               remove: ALWAYS
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableCityJsonConfiguration.Builder.class)
public interface CityJsonConfiguration extends ExtensionConfiguration, PropertyTransformations {

  enum Version {
    V10("1.0"),
    V11("1.1");

    private final String text;

    Version(String value) {
      text = value;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  /**
   * @langEn Enables support for CityJSON text sequences (media type `application/city+json-seq`).
   *     Requires version 1.1 or later.
   * @langDe Aktiviert die Unterstützung für CityJSON Text Sequences (Media-Type
   *     `application/city+json-seq`). Erfordert mindestens Version 1.1.
   * @default false
   * @since v3.3
   */
  Optional<Boolean> getTextSequences();

  /**
   * @langEn Select the CityJSON version that should be returned. Supported versions are `V10`
   *     (CityJSON 1.0) and `V11` (CityJSON 1.1).
   * @langDe Wählen Sie die CityJSON-Version, die zurückgegeben werden soll. Unterstützte Versionen
   *     sind `V10` (CityJSON 1.0) und `V11` (CityJSON 1.1).
   * @default V11
   * @since v3.3
   */
  Optional<Version> getVersion();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableCityJsonConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableCityJsonConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(
            PropertyTransformations.super
                .mergeInto((PropertyTransformations) source)
                .getTransformations())
        .build();
  }
}
