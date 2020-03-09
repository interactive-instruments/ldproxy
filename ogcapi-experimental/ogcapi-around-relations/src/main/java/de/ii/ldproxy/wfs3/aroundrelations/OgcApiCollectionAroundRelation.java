/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

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
public class OgcApiCollectionAroundRelation implements OgcApiCollectionExtension {

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, AroundRelationsConfiguration.class);
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
        final Optional<AroundRelationsConfiguration> aroundRelationConfiguration = featureTypeConfiguration.getExtension(AroundRelationsConfiguration.class);
        if (aroundRelationConfiguration.isPresent()) {
            if (!aroundRelationConfiguration.get()
                                            .getRelations()
                                            .isEmpty()) {
                collection.putExtensions("relations", aroundRelationConfiguration.get()
                                                                                 .getRelations()
                                                                                 .stream()
                                                                                 .collect(ImmutableMap.toImmutableMap(AroundRelationsConfiguration.Relation::getId, AroundRelationsConfiguration.Relation::getLabel)));
            }
        }
        return collection;
    }
}
