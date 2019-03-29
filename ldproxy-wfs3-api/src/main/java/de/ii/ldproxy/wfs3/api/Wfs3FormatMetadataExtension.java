package de.ii.ldproxy.wfs3.api;

import javax.ws.rs.core.Response;
import java.util.List;

public interface Wfs3FormatMetadataExtension extends Wfs3Extension {

    Wfs3MediaType getMediaType();

    Response getConformanceResponse(List<Wfs3ConformanceClass> wfs3ConformanceClasses, String serviceLabel,
                                    Wfs3MediaType wfs3MediaType, Wfs3MediaType[] alternativeMediaTypes,
                                    URICustomizer uriCustomizer, String staticUrlPrefix);

    Response getDatasetResponse(Wfs3Collections wfs3Collections, Wfs3ServiceData serviceData, Wfs3MediaType mediaType,
                                Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer,
                                String staticUrlPrefix, boolean isCollections);

    Response getCollectionResponse(Wfs3Collection wfs3Collection, Wfs3ServiceData serviceData, Wfs3MediaType mediaType,
                                   Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer,
                                   String collectionName);

    default boolean isEnabledForService(Wfs3ServiceData serviceData){return true;}
}
