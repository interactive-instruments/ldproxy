/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3DatasetMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Collection;
import java.util.List;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;

/**
 * add tiling information to the dataset metadata
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3DatasetMetadataVectorTiles implements Wfs3DatasetMetadataExtension{


    @Override
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3, Wfs3ServiceData serviceData){
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if(checkTilesEnabled(featureTypeConfigurationsWfs3,serviceData)){
            List<Wfs3Link> wfs3Links=vectorTilesLinkGenerator.generateDatasetLinks(uriCustomizer);
            collections.addLinks(wfs3Links.get(0));
            collections.addLinks(wfs3Links.get(1));
        }
        return collections;
    }

    /**
     * Check if the Tiles Extension is at least enabled in one collection
     *
     * @param featureTypeConfigurationsWfs3     feature type Configuration of all feature Types of the dataset
     * @return true if tiles extension enabled
     */
    private boolean checkTilesEnabled(Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3, Wfs3ServiceData serviceData) { //check if endpoint is enabled
        for(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3:featureTypeConfigurationsWfs3){
            if(isExtensionEnabled(serviceData,featureTypeConfigurationWfs3, EXTENSION_KEY)){
                return true;
            }
        }
        return false;
    }
}
