package de.ii.ldproxy.wfs3.api;

/**
 * @author zahnen
 */
public interface Wfs3RequestContext {
    Wfs3MediaType getMediaType();

    URICustomizer getUriCustomizer();

    String getStaticUrlPrefix();
}
