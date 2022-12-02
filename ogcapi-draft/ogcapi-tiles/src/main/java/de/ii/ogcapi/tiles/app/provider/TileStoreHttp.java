/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStoreReadOnly;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileStoreHttp implements TileStoreReadOnly {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStoreHttp.class);
  private static final Map<MediaType, String> EXTENSIONS =
      ImmutableMap.of(
          new MediaType("application", "vnd.mapbox-vector-tile"),
          "pbf",
          new MediaType("image", "jpeg"),
          "jpeg",
          new MediaType("image", "png"),
          "png",
          new MediaType("image", "tiff"),
          "tiff",
          new MediaType("image", "webp"),
          "webp");

  private final Map<String, String> layerSources;

  public TileStoreHttp(Map<String, String> layerSources) {
    this.layerSources = layerSources;
  }

  // TODO
  @Override
  public boolean has(TileQuery tile) {
    return false;
  }

  // TODO: use HttpClient
  @Override
  public TileResult get(TileQuery tile) throws IOException {
    if (!layerSources.containsKey(tile.getLayer())) {
      return TileResult.notFound();
    }

    try {
      String url = getUrl(tile, layerSources.get(tile.getLayer()));

      Response response = ClientBuilder.newClient().target(url).request(tile.getMediaType()).get();

      if (response.getStatus() == 200) {
        return TileResult.found(response.readEntity(InputStream.class).readAllBytes());
      }

      return TileResult.error(
          String.format(
              "Could not get tile: %s %s",
              response.getStatus(),
              response.hasEntity()
                  ? new String(
                      response.readEntity(InputStream.class).readAllBytes(), StandardCharsets.UTF_8)
                  : ""));
    } catch (Throwable e) {
      return TileResult.error(e.getMessage());
    }
  }

  // TODO
  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    return Optional.empty();
  }

  private static String getUrl(TileQuery tile, String template) {
    return StringTemplateFilters.applyTemplate(
        template,
        Map.of(
                "layer",
                tile.getLayer(),
                "tileMatrixSet",
                tile.getTileMatrixSet().getId(),
                "tileMatrix",
                String.valueOf(tile.getLevel()),
                "tileRow",
                String.valueOf(tile.getRow()),
                "tileCol",
                String.valueOf(tile.getCol()),
                "fileExtension",
                EXTENSIONS.get(tile.getMediaType()))
            ::get);
  }
}
