package de.ii.ldproxy.wfs3.api;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public interface Wfs3ExtensionRegistry {
    List<Wfs3ConformanceClass> getConformanceClasses();

    Map<Wfs3MediaType, Wfs3OutputFormatExtension> getOutputFormats();

    List<Wfs3EndpointExtension> getEndpoints();
}
