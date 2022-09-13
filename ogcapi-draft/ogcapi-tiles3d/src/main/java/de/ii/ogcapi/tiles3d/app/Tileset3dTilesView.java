/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.ogcapi.tiles3d.domain.Tileset3dTiles;
import de.ii.xtraplatform.services.domain.GenericView;
import java.util.Optional;

public class Tileset3dTilesView extends GenericView {
  public final String title;
  public final MapClient mapClient;
  public final CesiumData3dTiles cesiumData;
  public final String urlPrefix;

  public Tileset3dTilesView(
      Tileset3dTiles tileset,
      OgcApi api,
      String collectionId,
      // String attribution,
      String urlPrefix) {
    super("/templates/globe", null);
    this.title = "3D Tiles";

    Tiles3dConfiguration tiles3dConfig =
        api.getData().getExtension(Tiles3dConfiguration.class, collectionId).orElseThrow();
    HtmlConfiguration htmlConfig =
        api.getData().getExtension(HtmlConfiguration.class, collectionId).orElseThrow();
    this.mapClient =
        new ImmutableMapClient.Builder()
            .type(Type.CESIUM)
            .backgroundUrl(
                Optional.ofNullable(htmlConfig.getLeafletUrl())
                    .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl()))
                    .map(
                        url ->
                            url.replace("{z}", "{TileMatrix}")
                                .replace("{y}", "{TileRow}")
                                .replace("{x}", "{TileCol}")))
            .attribution(tileset.getAsset().getCopyright().map(att -> att.replace("'", "\\'")))
            .build();

    String tilesetJson;
    try {
      tilesetJson =
          new ObjectMapper()
              .registerModule(new Jdk8Module())
              .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
              .writeValueAsString(tileset);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not write 3D Tiles tileset json.", e);
    }
    this.cesiumData =
        new CesiumData3dTiles(
            tilesetJson,
            api.getSpatialExtent(collectionId),
            tiles3dConfig.shouldClampToEllipsoid(),
            tiles3dConfig.getIonAccessToken(),
            tiles3dConfig.getMaptilerApiKey(),
            tiles3dConfig.getCustomTerrainProviderUri(),
            tiles3dConfig.getTerrainHeightDifference());
    this.urlPrefix = urlPrefix;
  }
}
