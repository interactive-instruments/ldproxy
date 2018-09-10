package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * TODO: this is just a placeholder.
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataVectorTiles implements Wfs3CollectionMetadataExtension {
    @Override
    public Wfs3Collection process(Wfs3Collection collection, URICustomizer uriCustomizer, boolean isNested) {

        if (!isNested) {
            //TODO
            // extend collection

            collection.setLinks(ImmutableList.<Wfs3Link>builder().addAll(collection.getLinks())
                                                                 .add(ImmutableWfs3Link.builder()
                                                                                       .rel("tiles")
                                                                                       .description("tiles")
                                                                                       .type("tiles")
                                                                                       .href(uriCustomizer.ensureLastPathSegment("tiles")
                                                                                                          .toString())
                                                                                       .build())
                                                                 .build());

            collection.addExtension("tiles", ImmutableMap.of());
        }

        return collection;
    }
}
