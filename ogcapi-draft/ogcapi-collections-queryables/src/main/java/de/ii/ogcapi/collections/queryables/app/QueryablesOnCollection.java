/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * add CRS information to the collection information
 */
@Singleton
@AutoBind
public class QueryablesOnCollection implements CollectionExtension {


    private final I18n i18n;

    @Inject
    public QueryablesOnCollection(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return QueryablesConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDataV2 apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (isExtensionEnabled(featureTypeConfiguration, QueryablesConfiguration.class) && !isNested) {
            final QueryablesLinkGenerator linkGenerator = new QueryablesLinkGenerator();
            collection.addAllLinks(linkGenerator.generateCollectionLinks(uriCustomizer, i18n, language));
        }

        return collection;
    }

}
