/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableWfs3Collection;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3CollectionMetadataExtension;
import de.ii.ldproxy.wfs3.core.Wfs3CoreConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

/**
 * @author zahnen
 */

@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataAroundRelation implements Wfs3CollectionMetadataExtension {

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return isExtensionEnabled(dataset, AroundRelationsConfiguration.class);
    }

    @Override
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection,
                                                   FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   URICustomizer uriCustomizer, boolean isNested,
                                                   OgcApiDatasetData datasetData) {
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
