/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.app;

import de.ii.ogcapi.features.core.domain.EncodingContextSfFlat;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface EncodingContextFlatgeobuf extends EncodingContextSfFlat {

  FeatureSchema getSchema();

  Optional<CrsTransformer> getCrsTransformer();

  boolean getIs3d();
}
