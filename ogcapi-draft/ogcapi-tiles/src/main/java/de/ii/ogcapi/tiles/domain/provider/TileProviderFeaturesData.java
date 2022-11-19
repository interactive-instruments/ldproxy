/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderFeaturesData.Builder;
import java.util.Map;
import java.util.Objects;
import org.immutables.value.Value;

/**
 * # Tile-Provider FEATURES
 *
 * @langEn In this tile provider, the tiles in Mapbox Vector Tiles format are derived from the
 *     features provided by the API in the area of the tile.
 * @langDe Bei diesem Tile-Provider werden die Kacheln im Format Mapbox Vector Tiles aus den von der
 *     API bereitgestellten Features im Gebiet der Kachel abgeleitet.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = Builder.class)
public interface TileProviderFeaturesData extends TileProviderData, WithCaches {

  String PROVIDER_SUBTYPE = "FEATURES";

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `FEATURES`
   */
  @Override
  default String getTileProviderType() {
    return PROVIDER_SUBTYPE;
  }

  @Value.Default
  default LayerOptionsFeaturesDefault getLayerDefaults() {
    return new ImmutableLayerOptionsFeaturesDefault.Builder().build();
  }

  // TODO: Buildable, merge defaults into layers
  Map<String, LayerOptionsFeatures> getLayers();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderFeaturesData)) return this;

    Builder builder =
        ImmutableTileProviderFeaturesData.builder()
            .from((TileProviderFeaturesData) source)
            .from(this);

    TileProviderFeaturesData src = (TileProviderFeaturesData) source;
    /*
        List<String> tileEncodings =
            Objects.nonNull(src.getTileEncodings())
                ? Lists.newArrayList(src.getTileEncodings())
                : Lists.newArrayList();
        getTileEncodings()
            .forEach(
                tileEncoding -> {
                  if (!tileEncodings.contains(tileEncoding)) {
                    tileEncodings.add(tileEncoding);
                  }
                });
        builder.tileEncodings(tileEncodings);

        Map<String, MinMax> mergedSeeding =
            Objects.nonNull(src.getSeeding())
                ? Maps.newLinkedHashMap(src.getSeeding())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding())) getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels =
            Objects.nonNull(src.getZoomLevels())
                ? Maps.newLinkedHashMap(src.getZoomLevels())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels())) getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        if (!getCenter().isEmpty()) builder.center(getCenter());
        else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

        Map<String, MinMax> mergedZoomLevelsCache =
            Objects.nonNull(src.getZoomLevelsCache())
                ? Maps.newLinkedHashMap(src.getZoomLevelsCache())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevelsCache()))
          getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules =
            Objects.nonNull(src.getRules())
                ? Maps.newLinkedHashMap(src.getRules())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getRules())) getRules().forEach(mergedRules::put);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters =
            Objects.nonNull(src.getFilters())
                ? Maps.newLinkedHashMap(src.getFilters())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getFilters())) getFilters().forEach(mergedFilters::put);
        builder.filters(mergedFilters);

        if (Objects.nonNull(getCenter())) builder.center(getCenter());
        else if (Objects.nonNull(src.getCenter())) builder.center(src.getCenter());
    */
    return builder.build();
  }
}
