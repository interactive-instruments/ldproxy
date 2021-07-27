/**
 * Copyright 2021 interactive instruments GmbH
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

    FeatureCollectionView collection();

    MustacheRenderer mustacheRenderer();

    @Value.Derived
    default boolean isSchemaOrgEnabled() {
        return Objects.equals(htmlConfiguration().getSchemaOrgEnabled(), true);
    }

    @Value.Derived
    default FeaturesHtmlConfiguration featuresHtmlConfiguration() {
        Optional<FeaturesHtmlConfiguration> collectionHtmlConfiguration = Optional.ofNullable(getApiData().getCollections()
                                                                                                  .get(getCollectionId()))
                                                                          .flatMap(featureTypeConfiguration -> featureTypeConfiguration.getExtension(FeaturesHtmlConfiguration.class));

        if (collectionHtmlConfiguration.isPresent()) {
            return collectionHtmlConfiguration.get();
        }

        Optional<FeaturesHtmlConfiguration> baseHtmlConfiguration = getApiData().getExtension(FeaturesHtmlConfiguration.class);

        if (baseHtmlConfiguration.isPresent()) {
            return baseHtmlConfiguration.get();
        }

        return null;
    }

    @Value.Derived
    default HtmlConfiguration htmlConfiguration() {
        Optional<HtmlConfiguration> collectionHtmlConfiguration = Optional.ofNullable(getApiData().getCollections()
            .get(getCollectionId()))
            .flatMap(featureTypeConfiguration -> featureTypeConfiguration.getExtension(HtmlConfiguration.class));

        if (collectionHtmlConfiguration.isPresent()) {
            return collectionHtmlConfiguration.get();
        }

        Optional<HtmlConfiguration> baseHtmlConfiguration = getApiData().getExtension(HtmlConfiguration.class);

        if (baseHtmlConfiguration.isPresent()) {
            return baseHtmlConfiguration.get();
        }

        return null;
    }
}
