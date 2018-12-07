package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collections;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.core.Wfs3DatasetMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Collection;
import java.util.List;

/**
 * add styles information to the dataset metadata
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3DatasetMetdataStyles implements Wfs3DatasetMetadataExtension {


    @Override
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3){
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<Wfs3Link> wfs3Links=stylesLinkGenerator.generateDatasetLinks(uriCustomizer);
        collections.addLinks(wfs3Links.get(0));

        return collections;
    }
}
