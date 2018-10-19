package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

                final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
                List<Map<String, Object>> wfs3LinksList = new ArrayList<>();
                Map<String, Object> wfs3LinksMap = new HashMap<>();
          //  for(Object tilingSchemeId : TODO tilingSchemeIDs) {

                wfs3LinksMap.put("identifier", "default"); //TODO replace with tilingSchemeId
                wfs3LinksMap.put("links", wfs3LinksGenerator.generateTilesLinks(uriCustomizer, "default")); //TODO replace with tilingSchemeId
                wfs3LinksList.add(wfs3LinksMap);

        //    }
            collection.addExtension("tilingSchemes",ImmutableList.of(wfs3LinksList));
        }

        return collection;
    }
}
