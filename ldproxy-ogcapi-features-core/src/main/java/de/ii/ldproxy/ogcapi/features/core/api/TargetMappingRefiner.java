/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;

public interface TargetMappingRefiner {

    boolean needsRefinement(SourcePathMapping sourcePathMapping);

    SourcePathMapping refine(SourcePathMapping sourcePathMapping, SimpleFeatureGeometry simpleFeatureGeometry,
                             boolean mustReversePolygon);
}
