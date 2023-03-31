/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.docs.DocIgnore;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock ROUTING
 * @examplesAll
 *     <p><code>
 * ```yaml
 * - buildingBlock: ROUTING
 *   enabled: true
 *   featureType: route
 *   defaultCrs: CRS84
 *   coordinatePrecision:
 *     metre: 2
 *     degree: 7
 *   defaultPreference: fastest
 *   defaultMode: driving
 *   speedLimitUnit: kmph
 *   intermediateWaypoints: true
 *   weightRestrictions: true
 *   heightRestrictions: true
 *   obstacles: true
 *   manageRoutes: true
 *   html:
 *     enabled: true
 *     crs:
 *       'WGS 84 longitude/latitude':
 *         code: 4326
 *         forceAxisOrder: LON_LAT
 *       'Web Mercator':
 *         code: 3857
 *         forceAxisOrder: NONE
 *     defaults:
 *       name: Calexico International Airport to Shafter Airport
 *       start:
 *       - -115.510
 *       - 32.667
 *       end:
 *       - -119.184
 *       - 35.505
 *       center:
 *       - -118.0
 *       - 34.5
 *       centerLevel: 6
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutingConfiguration.Builder.class)
public interface RoutingConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn *Required* Name of the feature type in the [feature provider with the routing
   *     extension](../../providers/feature/extensions/routing.html) that provides the route feature
   *     as a series of route segments. See [Routing Provider](#routing-provider) for configuring
   *     the
   * @langDe *Required* Name der Objektart im [Feature-Provider mit der
   *     Routing-Erweiterung](../../providers/feature/extensions/routing.html), die eine Route als
   *     eine Reihe von Routenabschnitten bereitstellt.
   * @default null
   * @since v3.1
   */
  @Nullable
  String getFeatureType();

  /**
   * @langEn Enables support for conformance class *Manage Routes*. If enabled, routes along with
   *     their request payload are stored in the API and can be retrieved. Routes that are no longer
   *     needed can be deleted.
   * @langDe Aktiviert die Unterstützung für die Konformitätsklasse *Manage Routes*. Wenn diese
   *     Funktion aktiviert ist, werden die Routen zusammen mit der Nutzlast ihrer Anfrage in der
   *     API gespeichert und können abgerufen werden. Routen, die nicht mehr benötigt werden, können
   *     gelöscht werden.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getManageRoutes();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isManageRoutesEnabled() {
    return Objects.equals(getManageRoutes(), true);
  }

  /**
   * @langEn Enables support for conformance class *Intermediate Waypoints*. If enabled, routing
   *     requests can provide more than two waypoints along the route.
   * @langDe Aktiviert die Unterstützung für die Konformitätsklasse *Intermediate Waypoints*. Wenn
   *     diese Funktion aktiviert ist, können Routing-Anfragen mehr als zwei Wegpunkte entlang der
   *     Route enthalten.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getIntermediateWaypoints();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsIntermediateWaypoints() {
    return Objects.equals(getIntermediateWaypoints(), true);
  }

  /**
   * @langEn Enables support for conformance class *Weight Restrictions*. If enabled, routing
   *     requests can include the weight of the vehicle and route segments will only be selected, if
   *     they support the weight.
   * @langDe Aktiviert die Unterstützung der Konformitätsklasse *Weight Restrictions*. Wenn
   *     aktiviert, können Routing-Anfragen das Gewicht des Fahrzeugs enthalten und es werden nur
   *     Routensegmente ausgewählt, die das Gewicht unterstützen.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getWeightRestrictions();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsWeightRestrictions() {
    return Objects.equals(getWeightRestrictions(), true);
  }

  /**
   * @langEn Enables support for conformance class *Height Restrictions*. If enabled, routing
   *     requests can include the height of the vehicle and route segments will only be selected, if
   *     they support the weight.
   * @langDe Aktiviert die Unterstützung der Konformitätsklasse *Height Restrictions*. Wenn
   *     aktiviert, können Routing-Anfragen die Höhe des Fahrzeugs enthalten und es werden nur
   *     Routensegmente ausgewählt, die die Höhe erlauben.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getHeightRestrictions();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsHeightRestrictions() {
    return Objects.equals(getHeightRestrictions(), true);
  }

  /**
   * @langEn Enables support for conformance class *Obstacles*. If enabled, routing requests can
   *     include a multi-polygon of areas that the route must avoid.
   * @langDe Aktiviert die Unterstützung für die Konformitätsklasse *Obstacles*. Wenn diese Funktion
   *     aktiviert ist, können Routing-Anfragen ein Multi-Polygon von Bereichen enthalten, die die
   *     Route vermeiden muss.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getObstacles();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean supportsObstacles() {
    return Objects.equals(getObstacles(), true);
  }

  /**
   * @langEn Sets the unit used in speed limit attributes of segments. Either "kmph" or "mph".
   * @langDe Legt die in den Attributen für die Geschwindigkeitsbegrenzung von Segmenten verwendete
   *     Einheit fest. Entweder "kmph" oder "mph".
   * @default kmph
   * @since v3.1
   */
  @Nullable
  String getSpeedLimitUnit();

  /**
   * @langEn Sets the default value for the "preference" (cost function) in the routing request.
   * @langDe Legt den Standardwert für die "preference" (Kostenfunktion) in der Routing-Anfrage
   *     fest.
   * @default fastest
   * @since v3.1
   */
  String getDefaultPreference();

  /**
   * @langEn Sets the default value for the "mode" (mode of transport) in the routing request.
   * @langDe Legt den Standardwert für den "mode" (das Verkehrsmittel) in der Routing-Anfrage fest.
   * @default driving
   * @since v3.1
   */
  String getDefaultMode();

  /**
   * @langEn The routing provider supports passing additional flags (key-value-pairs) that can be
   *     taken into account in the routing process.
   * @langDe Der Routing-Provider unterstützt die Angabe zusätzlicher Flags (Key-Value-Paare), die
   *     beim Routing-Prozess berücksichtigt werden können.
   * @default {}
   * @since v3.1
   */
  // Not supported by standard pgRouting functions, ignore in documentation
  @DocIgnore
  Map<String, RoutingFlag> getAdditionalFlags();

  /**
   * @langEn Default coordinate reference system, either `CRS84` for routing providers with 2D
   *     coordinates, `CRS84h` for 3D coordinates.
   * @langDe Setzt das Standard-Koordinatenreferenzsystem, entweder 'CRS84' für einen
   *     Routing-Provider mit 2D-Koordinaten oder 'CRS84h' bei 3D-Koordinaten.
   * @default CRS84
   * @since v3.1
   */
  @Nullable
  FeaturesCoreConfiguration.DefaultCrs getDefaultCrs();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default EpsgCrs getDefaultEpsgCrs() {
    return ImmutableEpsgCrs.copyOf(
        getDefaultCrs() == FeaturesCoreConfiguration.DefaultCrs.CRS84h
            ? OgcCrs.CRS84h
            : OgcCrs.CRS84);
  }

  /**
   * @langEn Controls whether coordinates are limited to a certain number of places depending on the
   *     coordinate reference system used. The unit of measurement and the corresponding number of
   *     decimal places must be specified. Example: `{ "metre" : 2, "degree" : 7 }`. Valid units of
   *     measurement are "metre" and "degree".
   * @langDe Steuert, ob Koordinaten in Abhängig des verwendeten Koordinatenreferenzsystems auf eine
   *     bestimmte Anzahl von Stellen begrenzt werden. Anzugeben ist die Maßeinheit und die
   *     zugehörige Anzahl der Nachkommastellen. Beispiel: `{ "metre" : 2, "degree" : 7 }`. Gültige
   *     Maßeinheiten sind "metre" (bzw. "meter") und "degree".
   * @default {}
   * @since v3.1
   */
  Map<String, Integer> getCoordinatePrecision();

  /**
   * @langEn If set, route segment geometries with 3D coordinates will be smoothened using the
   *     Douglas-Peucker algorithm with the specified tolerance.
   * @langDe Wenn diese Option gesetzt ist, werden Routensegmente mit 3D-Koordinaten unter
   *     Verwendung des Douglas-Peucker-Algorithmus mit der angegebenen Toleranz geglättet.
   * @default null
   * @since v3.1
   */
  @Nullable
  Double getElevationProfileSimplificationTolerance();

  /**
   * @langEn If enabled (the object includes a "enabled" member set to `true`), the HTML response to
   *     the Get Routes operation will be enabled. The object can also include a "defaults" member
   *     that contains key-value pairs. The following properties are supported: "name" (default name
   *     of the route), "start" (longitude and latitude of the default start of the route), "end"
   *     (longitude and latitude of the default end of the route), "center" (longitude and latitude
   *     of the center point of the initial map view), "centerLevel" (WebMercatorQuad zoom level of
   *     the initial map view).
   * @langDe Wenn aktiviert (das Objekt enthält ein "enabled"-Member, das auf `true` gesetzt ist),
   *     wird die HTML-Antwort auf die Operation "Get Routes" aktiviert. Das Objekt kann außerdem
   *     ein "defaults"-Mitglied enthalten, das Key-Value-Paare enthält. Die folgenden Eigenschaften
   *     werden unterstützt: "name" (Standardname der Route), "start" (Längen- und Breitengrad des
   *     Standard-Startpunkts der Route), "end" (Längen- und Breitengrad des Standard-Endpunkts der
   *     Route), "center" (Längen- und Breitengrad des Mittelpunkts der ursprünglichen
   *     Kartenansicht), "centerLevel" (WebMercatorQuad-Zoomstufe der ursprünglichen Kartenansicht).
   * @default { "enabled": true }
   * @since v3.1
   */
  @Nullable
  HtmlForm getHtml();

  @Override
  default Builder getBuilder() {
    return new ImmutableRoutingConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableRoutingConfiguration.Builder builder =
        ((ImmutableRoutingConfiguration.Builder) source.getBuilder()).from(source).from(this);

    RoutingConfiguration src = (RoutingConfiguration) source;

    // always override the default configuration options
    builder.additionalFlags(getAdditionalFlags());

    return builder.build();
  }
}
