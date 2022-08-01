/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.domain;

import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import java.util.Map;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGmlUpgrade
    implements FeatureTransformationContext {

  public abstract Map<String, String> getNamespaces();
}
