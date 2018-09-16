package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * add tiling information to the collection metadata (supported tiling schemes, links)
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataVectorTiles implements Wfs3CollectionMetadataExtension {
    @Override
    public Wfs3Collection process(Wfs3Collection collection, URICustomizer uriCustomizer, boolean isNested) {


        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        if (!isNested) {
            collection.setLinks(ImmutableList.<Wfs3Link>builder().addAll(collection.getLinks())
                                                                 .add(ImmutableWfs3Link.builder()
                                                                                       .rel("tiles")
                                                                                       .description(collection.getTitle()+" as Mapbox vector tiles. The link is a URI template where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' property, {level}/{row}/{col} the tile based on the tiling scheme.")
                                                                                       .type(Wfs3MediaTypes.MVT)
                                                                                       .href(uriCustomizer.ensureLastPathSegment("tiles/")
                                                                                                          .toString()+
                                                                                               "{tilingSchemeId}/{level}/{row}/{col}?f=mvt")
                                                                                       .build())
                                                                 .add(ImmutableWfs3Link.builder()
                                                                                       .rel("tiles")
                                                                                       .description(collection.getTitle()+" as tiles in GeoJSON. The link is a URI template where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' property, {level}/{row}/{col} the tile based on the tiling scheme.")
                                                                                       .type(Wfs3MediaTypes.GEO_JSON)
                                                                                       .href(uriCustomizer.toString()+
                                                                                               "{tilingSchemeId}/{level}/{row}/{col}?f=json")
                                                                                       .build())
                                                                 .build());

            collection.addExtension("tilingSchemes", ImmutableList.of( "default" ));
        }

        return collection;
    }
}
