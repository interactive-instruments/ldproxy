package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3DatasetMetadataExtension;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.core.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;

import static de.ii.ldproxy.wfs3.styles.StylesConfiguration.EXTENSION_KEY;

/**
 * add styles information to the dataset metadata
 *
 */


@Component
@Provides
@Instantiate
public class Wfs3DatasetMetdataStyles implements Wfs3DatasetMetadataExtension {

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3, Wfs3ServiceData serviceData){
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<Wfs3Link> wfs3Links=stylesLinkGenerator.generateDatasetLinks(uriCustomizer);
        collections.addLinks(wfs3Links.get(0));

        List<String> stylesList = keyValueStore.getChildStore("styles").getChildStore(serviceData.getId()).getKeys();

        if(isExtensionEnabled(serviceData,EXTENSION_KEY)){

            StylesConfiguration stylesExtension= (StylesConfiguration) getExtensionConfiguration(serviceData,EXTENSION_KEY).get();
            if(stylesExtension.getMapsEnabled()){
                for(String style : stylesList){
                    String styleId=style.split("\\.")[0];
                    collections.addStyles(new Wfs3Style(styleId,coreServerConfig.getExternalUrl() + "/" + serviceData.getId()+"/maps/" + styleId));
                }
            }
        }

        return collections;
    }
}
