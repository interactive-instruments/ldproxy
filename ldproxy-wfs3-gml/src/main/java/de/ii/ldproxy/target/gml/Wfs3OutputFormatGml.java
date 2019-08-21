/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ConformanceClasses;
import de.ii.ldproxy.ogcapi.domain.Dataset;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatGml implements ConformanceClass, Wfs3OutputFormatExtension {

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .main(new MediaType("application", "gml+xml", ImmutableMap.of("version", "3.2", "profile", "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
            .label("GML")
            .metadata(MediaType.APPLICATION_XML_TYPE)
            .build();

    @Requires
    private GmlConfig gmlConfig;

    @ServiceController(value = false)
    private boolean enable;

    @Validate
    private void onStart() {
        this.enable = gmlConfig.isEnabled();
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf2";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, GmlConfiguration.class);
    }

    @Override
    public Response getConformanceResponse(List<ConformanceClass> wfs3ConformanceClasses, String serviceLabel,
                                           OgcApiMediaType ogcApiMediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                           URICustomizer uriCustomizer, String staticUrlPrefix) {
        return response(new Wfs3ConformanceClassesXml(new ConformanceClasses(wfs3ConformanceClasses.stream()
                                                                                                   .map(ConformanceClass::getConformanceClass)
                                                                                                   .collect(Collectors.toList()))));
    }

    @Override
    public Response getDatasetResponse(Dataset dataset, OgcApiDatasetData datasetData, OgcApiMediaType mediaType,
                                       List<OgcApiMediaType> alternativeMediaTypes, URICustomizer uriCustomizer,
                                       String staticUrlPrefix, boolean isCollections) {
        if (isCollections) {
            return response(new Wfs3CollectionsXml(dataset));
        }

        return response(new LandingPage(dataset.getLinks()));
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, OgcApiDatasetData datasetData,
                                          OgcApiMediaType mediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                          URICustomizer uriCustomizer, String collectionName) {
        return response(new Wfs3CollectionXml(wfs3Collection));
    }

    @Override
    public boolean canPassThroughFeatures() {
        return true;
    }

    @Override
    public Optional<GmlConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.of(new FeatureTransformerGmlUpgrade(ImmutableFeatureTransformationContextGml.builder()
                                                                                                    .from(transformationContext)
                                                                                                    .namespaces(((ConnectionInfoWfsHttp) transformationContext.getServiceData()
                                                                                                                                                              .getFeatureProvider()
                                                                                                                                                              .getConnectionInfo())
                                                                                                            .getNamespaces())
                                                                                                    .build()));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }

    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }
}
