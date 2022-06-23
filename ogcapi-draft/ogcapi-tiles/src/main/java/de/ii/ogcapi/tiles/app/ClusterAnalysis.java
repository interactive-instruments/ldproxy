/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.ii.ogcapi.tiles.domain.MvtFeature;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;

class ClusterAnalysis {

  Multimap<MvtFeature, MvtFeature> clusters = ArrayListMultimap.create();
  Map<MvtFeature, MvtFeature> inCluster = new HashMap<>();
  Set<MvtFeature> standalone = new HashSet<>();

  static ClusterAnalysis analyse(List<MvtFeature> features, boolean boundary) {
    // determine clusters of connected features
    ClusterAnalysis clusterResult = new ClusterAnalysis();
    for (int i = 0; i < features.size(); i++) {
      MvtFeature fi = features.get(i);
      Geometry gi = fi.getGeometry();
      Optional<MvtFeature> cluster = Optional.ofNullable(clusterResult.inCluster.get(fi));
      for (int j = i + 1; j < features.size(); j++) {
        MvtFeature fj = features.get(j);
        Geometry gj = fj.getGeometry();
        boolean clustered =
            boundary ? gi.getBoundary().intersects(gj.getBoundary()) : gi.intersects(gj);
        if (clustered) {
          if (cluster.isPresent()) {
            // already in a cluster, add to the new feature to the cluster
            clusterResult.clusters.put(cluster.get(), fj);
            clusterResult.inCluster.put(fj, cluster.get());
          } else {
            // new cluster
            clusterResult.clusters.put(fi, fj);
            clusterResult.inCluster.put(fj, fi);
          }
        }
      }
      // if the feature wasn't already in a cluster and hasn't started a new cluster, it is
      // standalone
      if (cluster.isEmpty() && !clusterResult.clusters.containsKey(fi)) {
        clusterResult.standalone.add(fi);
      }
    }
    return clusterResult;
  }
}
