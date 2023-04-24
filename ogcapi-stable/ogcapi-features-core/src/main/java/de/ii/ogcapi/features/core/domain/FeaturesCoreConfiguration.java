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
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
   * @default true
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @langEn Id of the feature provider to use. Normally the feature provider and API ids are the
   *     same.
   * @langDe Identifiziert den verwendeten Feature-Provider. Standardmäßig besitzt der
   *     Feature-Provider dieselbe ID wie die API.
   * @default apiId
   */
  Optional<String> getFeatureProvider();

  /**
   * @langEn Id of the feature type to use as defined in the given feature provider. Normally the
   *     feature type and collection ids are the same.
   * @langDe Identifiziert die verwendete Objektart im Feature-Provider. Standardmäßig besitzt die
   *     Objektart dieselbe ID wie die Collection.
   * @default collectionId
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
   * @default CRS84
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
   * @default []
   */
  Set<String> getEmbeddedFeatureLinkRels();

  /**
   * @langEn Always add `self` link to features, even in the *Features* resource.
   * @langDe Steuert, ob in Features immer, auch in der Features-Ressourcen, ein `self`-Link
   *     enthalten ist.
   * @default false
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
   * @langEn *Deprecated* Use [Module Feature Collections -
   *     Queryables](collections_-_queryables.md). Controls which of the attributes in queries can
   *     be used for filtering data. A distinction is made between spatial (`spatial`), temporal
   *     (`temporal`) and "regular" (`q`, `other`) attributes. The attributes under `spatial` must
   *     be of type `GEOMETRY` in the provider schema, the attributes under `temporal` of type
   *     `DATETIME` or `DATE`. The searchable attributes are each listed by their name in an array.
   *     The queryables can be used in filter expressions ([building block "filter"](filter.md)).
   *     The primary spatial and temporal attributes (see provider configuration) can be used for
   *     selection via the [parameters
   *     `bbox`](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameter_bbox) and [parameters
   *     `datetime`](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameter_datetime),
   *     respectively. The remaining attributes are defined as [additional parameters for the
   *     respective feature
   *     collections](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameters_for_filtering_on_feature_properties)
   *     ("*" can be used as wildcard). In this way a selection of objects is already possible
   *     without additional building blocks. The attributes under `q` are also taken into account in
   *     the free text search in the query parameter with the same name.
   * @langDe Steuert, welche der Attribute in Queries für die Filterung von Daten verwendet werden
   *     können. Unterschieden werden räumliche (`spatial`), zeitliche (`temporal`) und "normale"
   *     (`q`, `other`) Attribute. Die Attribute unter `spatial` müssen im Provider-Schema vom Typ
   *     `GEOMETRY`, die Attribute unter `temporal` vom Typ `DATETIME` oder `DATE` sein. Die
   *     suchbaren Attribute werden jeweils über ihren Namen in einem Array aufgelistet. Die
   *     Queryables können in Filter-Ausdrücken ([Modul "Filter"](filter.md)) genutzt werden. Die
   *     primären räumlichen und zeitlichen Attribute (siehe Provider-Konfiguration) können über die
   *     [Parameter `bbox`](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameter_bbox) bzw.
   *     [Parameter `datetime`](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameter_datetime)
   *     für die Selektion verwendet werden. Die übrigen Attribute werden als [zusätzliche Parameter
   *     für die jeweilige Feature
   *     Collections](https://docs.ogc.org/is/17-069r4/17-069r4.html#_parameters_for_filtering_on_feature_properties)
   *     definiert ("*" kann als Wildcard verwendet werden). Auf diese Weise ist eine Selektion von
   *     Objekten bereits ohne zusätzliche Module möglich. Die Attribute unter `q` werden außerdem
   *     bei der freien Textsuche im Query-Parameter mit demselben Namen berücksichtigt.
   * @default {}
   */
  @Deprecated(since = "3.4.0")
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
   * @default {}
   */
  Map<String, Integer> getCoordinatePrecision();

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
  @Deprecated(since = "3.4.0")
  default boolean hasDeprecatedQueryables() {
    return getQueryables().orElse(FeaturesCollectionQueryables.of()).getAll().stream()
        .anyMatch(key -> key.matches(".*\\[[^]]*].*"));
  }

  @Deprecated(since = "3.4.0")
  default List<String> normalizeQueryables(List<String> queryables, String collectionId) {
    return queryables.stream()
        .map(
            queryable -> {
              if (queryable.matches(".*\\[[^]]*].*")) {
                LOGGER.warn(
                    "The queryable '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration.",
                    queryable,
                    collectionId);
                return queryable.replaceAll("\\[[^]]*]", "");
              }
              return queryable;
            })
        .collect(Collectors.toUnmodifiableList());
  }

  @Deprecated(since = "3.4.0")
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
