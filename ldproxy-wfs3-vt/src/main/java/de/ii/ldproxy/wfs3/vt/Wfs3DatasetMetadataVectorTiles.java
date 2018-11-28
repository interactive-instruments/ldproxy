package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableMap;
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
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3){
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if(checkTilesEnabled(featureTypeConfigurationsWfs3)){
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
    private static boolean checkTilesEnabled(Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3) { //check if endpoint is enabled
        for(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3:featureTypeConfigurationsWfs3){
            if(featureTypeConfigurationWfs3.getExtensions().containsKey(EXTENSION_KEY)){
                return true;
            }
        }
        return false;
    }
}
