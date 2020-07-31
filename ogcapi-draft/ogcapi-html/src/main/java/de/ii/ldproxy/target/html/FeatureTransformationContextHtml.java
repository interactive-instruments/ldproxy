/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.xtraplatform.codelists.Codelist;
import io.dropwizard.views.ViewRenderer;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextHtml implements FeatureTransformationContext {

    public abstract FeatureCollectionView getFeatureTypeDataset();

    public abstract Map<String, Codelist> getCodelists();

    public abstract ViewRenderer getMustacheRenderer();

    @Value.Derived
    public HtmlConfiguration getHtmlConfiguration() {
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
