/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {OgcApiOutputFormatJson.class, CollectionsFormatExtension.class, CommonFormatExtension.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OgcApiOutputFormatJson implements CollectionsFormatExtension, CommonFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaLandingPage;
    public final static String SCHEMA_REF_LANDING_PAGE = "#/components/schemas/LandingPage";
    private final Schema schemaConformance;
    public final static String SCHEMA_REF_CONFORMANCE = "#/components/schemas/ConformanceDeclaration";
    private final Schema schemaCollections;
    public final static String SCHEMA_REF_COLLECTIONS = "#/components/schemas/Collections";
    private final Schema schemaCollection;
    public final static String SCHEMA_REF_COLLECTION = "#/components/schemas/Collection";

    public OgcApiOutputFormatJson() {
        schemaLandingPage = schemaGenerator.getSchema(LandingPage.class);
        schemaConformance = schemaGenerator.getSchema(ConformanceDeclaration.class);
        schemaCollections = schemaGenerator.getSchema(Collections.class);
        schemaCollection = schemaGenerator.getSchema(OgcApiCollection.class);
    }

    @Override
    public String getPathPattern() {
        return "^\\/?(?:conformance|collections(/[\\w\\-]+)?)?$";
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                        .schema(schemaLandingPage)
                        .schemaRef(SCHEMA_REF_LANDING_PAGE)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();
        else if (path.equals("/conformance"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                        .schema(schemaConformance)
                        .schemaRef(SCHEMA_REF_CONFORMANCE)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();
        else if (path.equals("/collections"))
                return new ImmutableOgcApiMediaTypeContent.Builder()
                        .schema(schemaCollections)
                        .schemaRef(SCHEMA_REF_COLLECTIONS)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();
        else if (path.matches("^/collections/[^//]+/?"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaCollection)
                    .schemaRef(SCHEMA_REF_COLLECTION)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new ServerErrorException("Unexpected path "+path,500);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getLandingPageResponse(LandingPage apiLandingPage, OgcApiApi api, OgcApiRequestContext requestContext) {
        return response(apiLandingPage);
    }

    @Override
    public Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiApi api, OgcApiRequestContext requestContext) {
        return response(conformanceDeclaration);
    }

    @Override
    public Response getCollectionsResponse(Collections collections, OgcApiApi api, OgcApiRequestContext requestContext) {
        return response(collections);
    }

    @Override
    public Response getCollectionResponse(OgcApiCollection ogcApiCollection,
                                          OgcApiApi api,
                                          OgcApiRequestContext requestContext) {
        return response(ogcApiCollection);
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
