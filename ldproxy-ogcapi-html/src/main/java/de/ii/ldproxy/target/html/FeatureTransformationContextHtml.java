/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.codelists.CodelistEntity;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import io.dropwizard.views.ViewRenderer;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextHtml implements FeatureTransformationContext {

    public abstract FeatureCollectionView getFeatureTypeDataset();

    public abstract CodelistEntity[] getCodelists();

    public abstract ViewRenderer getMustacheRenderer();
}
