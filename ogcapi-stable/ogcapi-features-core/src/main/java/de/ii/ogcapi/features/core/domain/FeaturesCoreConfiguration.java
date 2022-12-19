/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock FEATURES_CORE
 * @examplesEn Example of specifications in the configuration file for the entire API (or in
 *     defaults):
 *     <p><code>
 * ```yaml
 * - buildingBlock: FEATURES_CORE
 *   coordinatePrecision:
 *     metre: 2
 *     degree: 7
 * ```
 *     </code>
 *     <p>Example of the specifications in the configuration file for a feature collection:<code>
 * ```yaml
 * - buildingBlock: FEATURES_CORE
 *   enabled: true
 *   itemType: feature
 *   queryables:
 *     spatial:
 *     - geometry
 *     temporal:
 *     - date
 *     q:
 *     - name
 *     - region
 *     - subregion
 *     - cluster
 *     - village
 *     - searchfield1
 *     - searchfield2
 *     other:
 *     - registerId
 *     - area_ha
 *   embeddedFeatureLinkRels:
 *   - self
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API (oder in den
 *     Defaults):
 *     <p><code>
 * ```yaml
 * - buildingBlock: FEATURES_CORE
 *   coordinatePrecision:
 *     metre: 2
 *     degree: 7
 * ```
 *     </code>
 *     <p>Beispiel für die Angaben in der Konfigurationsdatei für eine Feature Collection:
 *     <p><code>
 * ```yaml
 * - buildingBlock: FEATURES_CORE
 *   coordinatePrecision:
 *     metre: 2
 *     degree: 7
 * ```
 *     </code>
 *     <p>Example of the specifications in the configuration file for a feature collection:<code>
 * ```yaml
 * - buildingBlock: FEATURES_CORE
 *   enabled: true
 *   itemType: feature
 *   queryables:
 *     spatial:
 *     - geometry
 *     temporal:
 *     - date
 *     q:
 *     - name
 *     - region
 *     - subregion
 *     - cluster
 *     - village
 *     - searchfield1
 *     - searchfield2
 *     other:
 *     - registerId
 *     - area_ha
 *   embeddedFeatureLinkRels:
 *   - self
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesCoreConfiguration.Builder.class)
public interface FeaturesCoreConfiguration
    extends ExtensionConfiguration, PropertyTransformations, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  enum DefaultCrs {
    CRS84,
    CRS84h
  }

  enum ItemType {
    unknown,
    feature,
    record
  }

  int MINIMUM_PAGE_SIZE = 1;
  int DEFAULT_PAGE_SIZE = 10;
  int MAX_PAGE_SIZE = 10000;
  String PARAMETER_BBOX = "bbox";
  String PARAMETER_DATETIME = "datetime";
  String DATETIME_INTERVAL_SEPARATOR = "/";

  /**
   * @langEn Id of the feature provider to use. Normally the feature provider and API ids are the
   *     same.
   * @langDe Identifiziert den verwendeten Feature-Provider. Standardmäßig besitzt der
   *     Feature-Provider dieselbe ID wie die API.
   * @default API-ID
   */
  Optional<String> getFeatureProvider();

  /**
   * @langEn Id of the feature type to use as defined in the given feature provider. Normally the
   *     feature type and collection ids are the same.
   * @langDe Identifiziert die verwendete Objektart im Feature-Provider. Standardmäßig besitzt die
   *     Objektart dieselbe ID wie die Collection Diese Option ist nur im Kontext einer Feature
   *     Collection relevant.
   * @default Collection id
   */
  Optional<String> getFeatureType();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Value.Default
  default List<String> getFeatureTypes() {
    return getFeatureType().isPresent()
        ? ImmutableList.of(getFeatureType().get())
        : ImmutableList.of();
  }

  /**
   * @langEn Default coordinate reference system, either `CRS84` for datasets with 2D geometries or
   *     `CRS84h` for datasets with 3D geometries.
   * @langDe Setzt das Standard-Koordinatenreferenzsystem, entweder 'CRS84' für einen Datensatz mit
   *     2D-Geometrien oder 'CRS84h' für einen Datensatz mit 3D-Geometrien.
   * @default `CRS84h`
   */
  @Nullable
  DefaultCrs getDefaultCrs();

  /**
   * @langEn Minimum value for parameter `limit`.
   * @langDe Setzt den Minimalwert für den Parameter `limit`.
   * @default 1
   */
  @Nullable
  Integer getMinimumPageSize();

  /**
   * @langEn Default value for parameter `limit`.
   * @langDe Setzt den Defaultwert für den Parameter `limit`.
   * @default 10
   */
  @Nullable
  Integer getDefaultPageSize();

  /**
   * @langEn Maximum value for parameter `limit`.
   * @langDe Setzt den Maximalwert für den Parameter `limit`.
   * @default 10000
   */
  @Nullable
  Integer getMaximumPageSize();

  /**
   * @langEn Controls which links should be specified for each feature in the Features resource, if
   *     these exist. The values are the link relation types to be included. By default, links such
   *     as `self` or `alternate` are omitted from features in a FeatureCollection, this option can
   *     be used to add them if needed.
   * @langDe Steuert, welche Links bei jedem Feature in der Ressource "Features" angegeben werden
   *     sollen, sofern vorhanden. Die Werte sind die Link-Relation-Types, die berücksichtigt werden
   *     sollen. Standardmäßig werden Links wie `self` oder `alternate` bei den Features in einer
   *     FeatureCollection weggelassen, mit dieser Option können Sie bei Bedarf ergänzt werden.
   * @default `[]`
   */
  Set<String> getEmbeddedFeatureLinkRels();

  /**
   * @langEn Always add `self` link to features, even in the *Features* resource.
   * @langDe Steuert, ob in Features immer, auch in der Features-Ressourcen, ein `self`-Link
   *     enthalten ist.
   * @default `false`
   */
  @Deprecated
  @Nullable
  Boolean getShowsFeatureSelfLink();

  /**
   * Validate the coordinates of the bbox or filter parameters against the domain of validity of the
   * coordinate reference system
   */
  @Nullable
  Boolean getValidateCoordinatesInQueries();

  Optional<ItemType> getItemType();

  /**
   * @langEn Feature properties that can be used in queries to select the returned features, split
   *     into `spatial`, `temporal` and `other`. Properties in `spatial` have to be of type
   *     `GEOMETRY` in the provider, properties in `temporal` of type `DATETIME`. Properties are
   *     listed in an array by name. Queryables can be used in filter expressions ([Filter -
   *     CQL](filter.md)) or as filter parameters according to [OGC API - Features - Part 1: Core
   *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0). The parameter
   *     [bbox](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_bbox) acts on the
   *     first spatial property. The parameter
   *     [datetime](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_datetime) acts on
   *     the first two temporal properties, which are interpreted as start and end of an interval.
   *     If only one temporal property is given, it is interpreted as instant. Other properties are
   *     added as [additional
   *     parameters](http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_parameters_for_filtering_on_feature_properties)
   *     for the collection ("*" can be used as wildcard). Using the described parameters allows
   *     selection of features without additional modules.
   * @langDe Steuert, welche der Attribute in Queries für die Filterung von Daten verwendet werden
   *     können. Unterschieden werden räumliche (`spatial`), zeitliche (`temporal`) und "normale"
   *     (`q`, `other`) Attribute. Die Attribute unter `spatial` müssen im Provider-Schema vom Typ
   *     `GEOMETRY`, die Attribute unter `temporal` vom Typ `DATETIME` oder `DATE` sein. Die
   *     suchbaren Attribute werden jeweils über ihren Namen in einem Array aufgelistet. Die
   *     Queryables können in Filter-Ausdrücken ([Modul "Filter - CQL"](filter.md)) genutzt werden.
   *     Die primären räumlichen und zeitlichen Attribute (siehe Provider-Konfiguration) können über
   *     die [Parameter `bbox`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_bbox)
   *     bzw. [Parameter
   *     `datetime`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_datetime) für
   *     die Selektion verwendet werden. Die übrigen Attribute werden als [zusätzliche Parameter für
   *     die jeweilige Feature
   *     Collections](http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_parameters_for_filtering_on_feature_properties)
   *     definiert ("*" kann als Wildcard verwendet werden). Auf diese Weise ist eine Selektion von
   *     Objekten bereits ohne zusätzliche Module möglich. Die Attribute unter `q` werden außerdem
   *     bei der freien Textsuche im Query-Parameter mit demselben Namen berücksichtigt.
   * @default `{}`
   */
  Optional<FeaturesCollectionQueryables> getQueryables();

  /**
   * @langEn Controls whether coordinates are limited to a certain number of places depending on the
   *     coordinate reference system used. The unit of measurement and the corresponding number of
   *     decimal places must be specified. Example: `{ "metre" : 2, "degree" : 7 }`. Valid units of
   *     measurement are "metre" and "degree".
   * @langDe Steuert, ob Koordinaten in Abhängig des verwendeten Koordinatenreferenzsystems auf eine
   *     bestimmte Anzahl von Stellen begrenzt werden. Anzugeben ist die Maßeinheit und die
   *     zugehörige Anzahl der Nachkommastellen. Beispiel: `{ "metre" : 2, "degree" : 7 }`. Gültige
   *     Maßeinheiten sind "metre" (bzw. "meter") und "degree".
   * @default `{}`
   */
  Map<String, Integer> getCoordinatePrecision();

  /**
   * @langEn Optional transformations for feature properties for all media types, see
   *     [transformations](general-rules.md#transformations).
   * @langDe Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in allen
   *     Datenformaten [transformiert](general-rules.md#transformations) werden.
   * @default `{}`
   */
  @Override
  Map<String, List<PropertyTransformation>> getTransformations();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default EpsgCrs getDefaultEpsgCrs() {
    return ImmutableEpsgCrs.copyOf(
        getDefaultCrs() == DefaultCrs.CRS84h ? OgcCrs.CRS84h : OgcCrs.CRS84);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Map<String, String> getAllFilterParameters() {
    if (getQueryables().isPresent()) {
      FeaturesCollectionQueryables queryables = getQueryables().get();
      Map<String, String> parameters = new LinkedHashMap<>();

      if (!queryables.getSpatial().isEmpty()) {
        parameters.put(PARAMETER_BBOX, queryables.getSpatial().get(0));
      } else {
        parameters.put(PARAMETER_BBOX, FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE);
      }

      if (queryables.getTemporal().size() > 1) {
        parameters.put(
            PARAMETER_DATETIME,
            String.format(
                "%s%s%s",
                queryables.getTemporal().get(0),
                DATETIME_INTERVAL_SEPARATOR,
                queryables.getTemporal().get(1)));
      } else if (!queryables.getTemporal().isEmpty()) {
        parameters.put(PARAMETER_DATETIME, queryables.getTemporal().get(0));
      } else {
        parameters.put(PARAMETER_DATETIME, FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE);
      }

      queryables.getSpatial().forEach(property -> parameters.put(property, property));
      queryables.getTemporal().forEach(property -> parameters.put(property, property));

      getFilterParameters().forEach(property -> parameters.put(property, property));

      return parameters;
    }

    return ImmutableMap.of(
        PARAMETER_BBOX, FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE,
        PARAMETER_DATETIME, FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<String> getFilterParameters() {
    if (getQueryables().isPresent()) {
      return Stream.concat(
              getQueryables().get().getQ().stream(), getQueryables().get().getOther().stream())
          .collect(Collectors.toList());
    }

    return ImmutableList.of();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean hasDeprecatedQueryables() {
    return getQueryables().orElse(FeaturesCollectionQueryables.of()).getAll().stream()
        .anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
  }

  default List<String> normalizeQueryables(List<String> queryables, String collectionId) {
    return queryables.stream()
        .map(
            queryable -> {
              if (queryable.matches(".*\\[[^\\]]*\\].*")) {
                // TODO use info for now, but upgrade to warn after some time
                LOGGER.info(
                    "The queryable '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration.",
                    queryable,
                    collectionId);
                return queryable.replaceAll("\\[[^\\]]*\\]", "");
              }
              return queryable;
            })
        .collect(Collectors.toUnmodifiableList());
  }

  default Optional<FeaturesCollectionQueryables> normalizeQueryables(String collectionId) {
    Optional<FeaturesCollectionQueryables> queryables = getQueryables();
    if (queryables.isPresent()) {
      List<String> spatial = normalizeQueryables(queryables.get().getSpatial(), collectionId);
      List<String> temporal = normalizeQueryables(queryables.get().getTemporal(), collectionId);
      List<String> q = normalizeQueryables(queryables.get().getQ(), collectionId);
      List<String> other = normalizeQueryables(queryables.get().getOther(), collectionId);
      queryables =
          Optional.of(
              new ImmutableFeaturesCollectionQueryables.Builder()
                  .spatial(spatial)
                  .temporal(temporal)
                  .q(q)
                  .other(other)
                  .build());
    }
    return queryables;
  }

  default List<String> removeQueryables(
      List<String> queryables, Collection<String> queryablesToRemove) {
    return queryables.stream()
        .filter(queryable -> !queryablesToRemove.contains(queryable))
        .collect(Collectors.toUnmodifiableList());
  }

  default Optional<FeaturesCollectionQueryables> removeQueryables(
      Collection<String> queryablesToRemove) {
    Optional<FeaturesCollectionQueryables> queryables = getQueryables();
    if (queryables.isPresent()) {
      List<String> spatial = removeQueryables(queryables.get().getSpatial(), queryablesToRemove);
      List<String> temporal = removeQueryables(queryables.get().getTemporal(), queryablesToRemove);
      List<String> q = removeQueryables(queryables.get().getQ(), queryablesToRemove);
      List<String> other = removeQueryables(queryables.get().getOther(), queryablesToRemove);
      queryables =
          Optional.of(
              new ImmutableFeaturesCollectionQueryables.Builder()
                  .spatial(spatial)
                  .temporal(temporal)
                  .q(q)
                  .other(other)
                  .build());
    }
    return queryables;
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableFeaturesCoreConfiguration.Builder().from(this);
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableFeaturesCoreConfiguration.Builder builder =
        new ImmutableFeaturesCoreConfiguration.Builder()
            .from(source)
            .from(this)
            .transformations(
                PropertyTransformations.super
                    .mergeInto((PropertyTransformations) source)
                    .getTransformations());

    if (getQueryables().isPresent()
        && ((FeaturesCoreConfiguration) source).getQueryables().isPresent()) {
      builder.queryables(
          getQueryables()
              .get()
              .mergeInto(((FeaturesCoreConfiguration) source).getQueryables().get()));
    }

    Map<String, Integer> mergedCoordinatePrecision =
        new LinkedHashMap<>(((FeaturesCoreConfiguration) source).getCoordinatePrecision());
    mergedCoordinatePrecision.putAll(getCoordinatePrecision());
    builder.coordinatePrecision(mergedCoordinatePrecision);

    // keep the rels from the parent configuration and just add new rels
    builder.embeddedFeatureLinkRels(
        ImmutableSet.<String>builder()
            .addAll(((FeaturesCoreConfiguration) source).getEmbeddedFeatureLinkRels())
            .addAll(this.getEmbeddedFeatureLinkRels())
            .build());

    return builder.build();
  }
}
