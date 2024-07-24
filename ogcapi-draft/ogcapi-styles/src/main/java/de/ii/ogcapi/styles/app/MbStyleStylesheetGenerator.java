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
import de.ii.ogcapi.styles.domain.MbStyleLayer.LayerType;
import de.ii.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.values.domain.AutoValueFactory;
import java.util.ArrayList;
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
    // TODO: get api from entityDataStore and extract collections

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
    Builder style = new Builder().version(8);

    // TODO: get api from entityDataStore and extract collections, then add layers to style

    // get api from entityDataStore
    OgcApiDataV2 apiData = entityDataStore.get(apiId);

    // extract collections
    Map<String, ?> collections = apiData.getCollections();

    // add a layer for each collection to the style
    for (String collectionName : collections.keySet()) {
      style.addLayers(
          ImmutableMbStyleLayer.builder()
              .id(collectionName)
              .type(LayerType.fill)
              .source(apiId)
              .sourceLayer(collectionName)
              .build());
    }

    return style.build();
  }
}
