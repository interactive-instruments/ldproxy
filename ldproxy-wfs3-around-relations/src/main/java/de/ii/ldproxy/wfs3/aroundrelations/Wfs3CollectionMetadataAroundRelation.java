/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collection;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3CollectionMetadataExtension;
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
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested, Wfs3ServiceData serviceData) {
        final Optional<AroundRelationsConfiguration> aroundRelationConfiguration = featureTypeConfigurationWfs3.getExtension(AroundRelationsConfiguration.class);
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
