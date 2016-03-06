package de.ii.ldproxy.output.jsonld;

import de.ii.ldproxy.output.html.Gml2MicrodataMapper;
import de.ii.ogc.wfs.proxy.WfsProxyService;

/**
 * @author zahnen
 */
public class Gml2JsonLdMapper extends Gml2MicrodataMapper {


    public static final String MIME_TYPE = "application/ld+json";

    public Gml2JsonLdMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }
}
