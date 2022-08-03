/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGltf implements FeatureTransformationContext {

  public abstract boolean getClampToGround();

  public abstract CrsTransformer getCrsTransformerCrs84hToEcef();
}
