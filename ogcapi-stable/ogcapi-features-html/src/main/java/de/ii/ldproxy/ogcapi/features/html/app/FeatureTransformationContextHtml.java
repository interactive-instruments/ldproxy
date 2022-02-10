/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.dropwizard.domain.MustacheRenderer;
import java.util.Objects;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface FeatureTransformationContextHtml extends FeatureTransformationContext {

    FeatureCollectionView collectionView();

    MustacheRenderer mustacheRenderer();

    @Value.Derived
    default boolean isSchemaOrgEnabled() {
        return Objects.equals(htmlConfiguration().getSchemaOrgEnabled(), true);
    }

    @Value.Derived
    default FeaturesHtmlConfiguration featuresHtmlConfiguration() {
        return getConfiguration(FeaturesHtmlConfiguration.class);
    }

    @Value.Derived
    default HtmlConfiguration htmlConfiguration() {
        return getConfiguration(HtmlConfiguration.class);
    }
}
