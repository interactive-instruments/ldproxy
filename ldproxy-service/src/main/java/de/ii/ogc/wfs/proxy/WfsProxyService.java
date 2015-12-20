package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;

import java.util.Map;

/**
 * @author zahnen
 */
public interface WfsProxyService {
    WFSAdapter getWfsAdapter();

    WFSProxyServiceProperties getServiceProperties();

    Map<String, WfsProxyFeatureType> getFeatureTypes();
}
