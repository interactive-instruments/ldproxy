/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.features.domain.DatasetChangeListener;
import de.ii.xtraplatform.features.domain.FeatureChangeHandler;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface WithChangeListeners {

  Map<String, DatasetChangeListener> datasetChangeListeners = new ConcurrentHashMap<>();
  Map<String, FeatureChangeListener> featureChangeListeners = new ConcurrentHashMap<>();

  DatasetChangeListener onDatasetChange(OgcApi api);

  FeatureChangeListener onFeatureChange(OgcApi api);

  default void updateChangeListeners(FeatureChangeHandler changeHandler, OgcApi api) {
    if (datasetChangeListeners.containsKey(api.getId())) {
      changeHandler.removeListener(datasetChangeListeners.get(api.getId()));
    }

    DatasetChangeListener datasetChangeListener = onDatasetChange(api);
    changeHandler.addListener(datasetChangeListener);
    datasetChangeListeners.put(api.getId(), datasetChangeListener);

    if (featureChangeListeners.containsKey(api.getId())) {
      changeHandler.removeListener(featureChangeListeners.get(api.getId()));
    }

    FeatureChangeListener featureChangeListener = onFeatureChange(api);
    changeHandler.addListener(featureChangeListener);
    featureChangeListeners.put(api.getId(), featureChangeListener);
  }

  default void removeChangeListeners(FeatureChangeHandler changeHandler, OgcApi api) {
    if (datasetChangeListeners.containsKey(api.getId())) {
      changeHandler.removeListener(datasetChangeListeners.get(api.getId()));
      datasetChangeListeners.remove(api.getId());
    }

    if (featureChangeListeners.containsKey(api.getId())) {
      changeHandler.removeListener(featureChangeListeners.get(api.getId()));
      featureChangeListeners.remove(api.getId());
    }
  }
}
