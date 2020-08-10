/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.ImmutableProcess;
import de.ii.ldproxy.ogcapi.features.processing.ImmutableProcessing;
import de.ii.ldproxy.ogcapi.features.processing.Processing;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormatProcessing;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class ObservationProcessingJson implements ObservationProcessingOutputFormatProcessing {

    @Requires
    SchemaGenerator schemaGenerator;

    @Requires
    I18n i18n;

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    private static final String DAPA_PATH_ELEMENT = "dapa";
    private final static String schemaRef = "#/components/schemas/"+DAPA_PATH_ELEMENT;

    public ObservationProcessingJson() {
        this.schema = schemaGenerator.getSchema(Processing.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        // get the collectionId from the path, [0] is empty, [1] is "collections"
        String collectionId = path.split("/", 4)[2];

        // get the processes from the API
        OgcApiEndpointDefinition definition = extensionRegistry.getExtensionsForType(OgcApiEndpointSubCollection.class)
                .stream()
                .filter(ext -> ext.getClass().getName().equals("de.ii.ldproxy.ogcapi.observation_processing.application.EndpointObservationProcessing"))
                .findAny()
                .map(ext -> ext.getDefinition(apiData))
                .orElse(null);
        Processing processList = ImmutableProcessing.builder()
                .title(i18n.get("processingTitle", Optional.empty()))
                .description(i18n.get("processingDescription", Optional.empty()))
                .endpoints(definition.getResources()
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        // reduce to two processes
                        .limit(2)
                        .map(entry -> {
                            final String subpath = entry.getKey();
                            final String id = subpath.substring(subpath.lastIndexOf("/")+1);
                            final OgcApiOperation op = entry.getValue().getOperations().get("GET");
                            final List<String> mediaTypes = op.getSuccess()
                                    .orElse(new ImmutableOgcApiResponse.Builder().description("").build())
                                    .getContent()
                                    .keySet()
                                    .stream()
                                    .map(mediaType -> mediaType.toString())
                                    .sorted()
                                    .collect(Collectors.toList());
                            if (op!=null)
                                return ImmutableProcess.builder()
                                        .id(id)
                                        .title(op.getSummary())
                                        .description(op.getDescription())
                                        .inputCollectionId(collectionId)
                                        .addLinks(new ImmutableOgcApiLink.Builder()
                                                .href(subpath)
                                                .title(i18n.get("dapaEndpointLink", Optional.empty()))
                                                .rel("ogc-dapa-endpoint")
                                                .build())
                                        .addLinks(new ImmutableOgcApiLink.Builder()
                                                .href("/api?f=json#/paths/" + path.replace("/","~1"))
                                                .title(i18n.get("dapaEndpointDefinitionLink", Optional.empty()))
                                                .rel("ogc-dapa-endpoint-definition")
                                                .build())
                                        .addLinks(new ImmutableOgcApiLink.Builder()
                                                .href("/api?f=html#/DAPA/get" + path.replaceAll("[/\\-:]","_"))
                                                .title(i18n.get("dapaEndpointDocumentationLink", Optional.empty()))
                                                .rel("ogc-dapa-endpoint-documentation")
                                                .build())
                                        .mediaTypes(mediaTypes)
                                        .externalDocs(op.getExternalDocs())
                                        .build();
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();
        // convert to JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectNode json = mapper.convertValue(processList, ObjectNode.class);

        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .addExamples(new ImmutableOgcApiExample.Builder().value(Optional.ofNullable(json)).build())
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public Object getEntity(Processing processList, String collectionId, OgcApiApi api, OgcApiRequestContext requestContext) {
        return processList;
    }
}
