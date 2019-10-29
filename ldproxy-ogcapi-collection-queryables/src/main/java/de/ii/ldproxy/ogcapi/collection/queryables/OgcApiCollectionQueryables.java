/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;


import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * add CRS information to the collection information
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionQueryables implements OgcApiCollectionExtension {

    @Requires
    I18n i18n;

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, QueryablesConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDatasetData apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (isExtensionEnabled(apiData, featureTypeConfiguration, QueryablesConfiguration.class) && !isNested) {
            final QueryablesLinkGenerator linkGenerator = new QueryablesLinkGenerator();
            collection.addAllLinks(linkGenerator.generateCollectionLinks(uriCustomizer, i18n, language));
        }

        return collection;
    }

}
