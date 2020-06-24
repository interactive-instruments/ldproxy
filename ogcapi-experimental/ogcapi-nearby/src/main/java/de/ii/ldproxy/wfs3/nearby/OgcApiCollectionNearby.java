/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.nearby;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiCollection;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionNearby implements OgcApiCollectionExtension {

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, NearbyConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), NearbyConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiApiDataV2 apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        final Optional<NearbyConfiguration> nearbyConfiguration = featureTypeConfiguration.getExtension(NearbyConfiguration.class);
        if (nearbyConfiguration.isPresent() && !isNested && nearbyConfiguration.get().getEnabled()) {
            if (!nearbyConfiguration.get()
                                            .getRelations()
                                            .isEmpty()) {
                collection.putExtensions("relations", nearbyConfiguration.get()
                                                                                 .getRelations()
                                                                                 .stream()
                                                                                 .collect(ImmutableMap.toImmutableMap(NearbyConfiguration.Relation::getId, NearbyConfiguration.Relation::getLabel)));
            }
        }
        return collection;
    }
}
