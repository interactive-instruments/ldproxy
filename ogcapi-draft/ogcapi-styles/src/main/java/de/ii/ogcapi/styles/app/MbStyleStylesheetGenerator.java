/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.styles.domain.ImmutableMbStyleLayer;
import de.ii.ogcapi.styles.domain.ImmutableMbStyleStylesheet.Builder;
import de.ii.ogcapi.styles.domain.ImmutableMbStyleVectorSource;
import de.ii.ogcapi.styles.domain.MbStyleLayer.LayerType;
import de.ii.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.values.domain.AutoValueFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MbStyleStylesheetGenerator
    implements AutoValueFactory<MbStyleStylesheet, String, List<String>> {

  private final EntityDataStore<OgcApiDataV2> entityDataStore;

  public MbStyleStylesheetGenerator(EntityDataStore<EntityData> entityDataStore) {
    this.entityDataStore = entityDataStore.forType(OgcApiDataV2.class);
  }

  @Override
  public Map<String, String> check(String apiId) {
    return Map.of();
  }

  @Override
  public List<String> analyze(String apiId) {

    if (!entityDataStore.has(apiId)) {
      throw new IllegalArgumentException("No API found with the id: " + apiId);
    }

    // get api from entityDataStore
    OgcApiDataV2 apiData = entityDataStore.get(apiId);

    // extract collections
    Map<String, ?> collections = apiData.getCollections();

    // convert the keys of the collections map to a list
    List<String> collectionNames = new ArrayList<>(collections.keySet());

    return collectionNames;
  }

  @Override
  public MbStyleStylesheet generate(String apiId, List<String> analyzeResult) {

    if (!entityDataStore.has(apiId)) {
      throw new IllegalArgumentException("No API found with the id: " + apiId);
    }

    Builder style = new Builder().version(8);

    // get api from entityDataStore
    OgcApiDataV2 apiData = entityDataStore.get(apiId);

    // extract collections
    Map<String, ?> collections = apiData.getCollections();

    // iterate over each collection
    for (String collectionName : collections.keySet()) {

      // add source for each collection
      style.putSources(
          collectionName,
          ImmutableMbStyleVectorSource.builder()
              .maxzoom(16)
              .tiles(
                  Collections.singletonList("{serviceUrl}/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt"))
              .build());

      // add layers for each collection
      style.addLayers(
          ImmutableMbStyleLayer.builder()
              .id(collectionName + ".fill")
              .type(LayerType.fill)
              .source(collectionName)
              .sourceLayer(collectionName)
              .putPaint("fill-color", "#7ac5a5")
              .build(),
          ImmutableMbStyleLayer.builder()
              .id(collectionName + ".line")
              .type(LayerType.line)
              .source(collectionName)
              .sourceLayer(collectionName)
              .putPaint("line-color", "#000000")
              .putPaint("line-width", 2)
              .build(),
          ImmutableMbStyleLayer.builder()
              .id(collectionName + ".circle")
              .type(LayerType.circle)
              .source(collectionName)
              .sourceLayer(collectionName)
              .putPaint("circle-radius", 3)
              .putPaint("circle-opacity", 0.5)
              .putPaint("circle-stroke-color", "#000000")
              .putPaint("circle-stroke-width", 1)
              .putPaint("circle-color", "#ffffff")
              .build());
    }

    return style.build();
  }
}
