package de.ii.ldproxy.service;

import de.ii.xsf.configstore.api.rest.ResourceStore;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface LdProxyServiceStore extends ResourceStore<LdProxyService> {
    LdProxyService addService(String id, String wfsUrl) throws IOException;
}
