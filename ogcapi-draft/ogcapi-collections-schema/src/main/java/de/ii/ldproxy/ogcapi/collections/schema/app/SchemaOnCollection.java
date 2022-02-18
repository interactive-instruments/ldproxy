/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaConfiguration;
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
 * add the schema link to the collection information
 */
@Singleton
@AutoBind
public class SchemaOnCollection implements CollectionExtension {

    private final I18n i18n;

    @Inject
    public SchemaOnCollection(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
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
        if (isExtensionEnabled(featureTypeConfiguration, SchemaConfiguration.class) && !isNested) {
            final SchemaLinkGenerator linkGenerator = new SchemaLinkGenerator();
            collection.addAllLinks(linkGenerator.generateCollectionLinks(uriCustomizer, featureTypeConfiguration.getLabel(), i18n, language));
        }

        return collection;
    }

}
