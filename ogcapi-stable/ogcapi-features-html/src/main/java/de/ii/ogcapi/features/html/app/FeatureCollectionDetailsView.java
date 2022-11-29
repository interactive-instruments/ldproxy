/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

@Value.Immutable
@Value.Style(builder = "new")
@Modifiable
public abstract class FeatureCollectionDetailsView extends FeatureCollectionView {

  FeatureCollectionDetailsView() {
    super("featureDetails.mustache");
  }
}
