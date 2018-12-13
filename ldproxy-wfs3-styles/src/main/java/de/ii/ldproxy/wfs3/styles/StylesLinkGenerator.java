package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;

import java.util.List;
/**
 * This class is responsible for generating the links to the styles.
 *
 */
public class StylesLinkGenerator {



    /**
     * generates the Links on the page /serviceId?f=json and /serviceId/collections?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a List with links
     */
    public List<Wfs3Link> generateDatasetLinks(URICustomizer uriBuilder) {

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();
        uriBuilder.ensureParameter("f", "json");

        builder.add(ImmutableWfs3Link.builder()
                .href(uriBuilder.copy()
                        .removeLastPathSegment("collections")
                        .ensureLastPathSegment("styles")
                        .setParameter("f", "json")
                        .toString())
                .rel("styles")
                .type("application/json")
                .description("the list of available styles")
                .build());

        return builder.build();
    }
    /**
     * generates one link of a style on the page /serviceId/styles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param styleId        the ids of the styles
     * @return a list with links
     */
    public List<Wfs3Link> generateStylesLinksDataset(URICustomizer uriBuilder, String styleId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("style")
                        .type("application/json")
                        .description(styleId)
                        .build())
                ;


        return builder.build();
    }

    /**
     * generates one link of a style on the page /serviceId/collections/{collectionId}/styles
     *
     * and in the collections styles link at /serviceid/collections and /servideid/collections/{collectionId}
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param styleId        the ids of the styles
     * @return a list with links
     */
    public List<Wfs3Link> generateStylesLinksCollection(URICustomizer uriBuilder, String styleId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("style")
                        .type("application/json")
                        .description(styleId)
                        .build())
                ;


        return builder.build();
    }
}
