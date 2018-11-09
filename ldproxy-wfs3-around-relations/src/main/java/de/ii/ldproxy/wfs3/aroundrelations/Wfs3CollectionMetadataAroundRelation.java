/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collection;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.aroundrelations.AroundRelationConfiguration.EXTENSION_KEY;

/**
 * @author zahnen
 */

@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataAroundRelation implements Wfs3CollectionMetadataExtension {
    @Override
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested) {
        if (featureTypeConfigurationWfs3.getExtensions().containsKey(EXTENSION_KEY)) {
            final AroundRelationConfiguration aroundRelationConfiguration = (AroundRelationConfiguration) featureTypeConfigurationWfs3.getExtensions().get(EXTENSION_KEY);
            if (!aroundRelationConfiguration.getRelations().isEmpty()) {
                collection.putExtensions("relations", aroundRelationConfiguration.getRelations().stream().map(AroundRelationConfiguration.Relation::getLabel).collect(Collectors.toList()));
            }
        }
        return collection;
    }
}
