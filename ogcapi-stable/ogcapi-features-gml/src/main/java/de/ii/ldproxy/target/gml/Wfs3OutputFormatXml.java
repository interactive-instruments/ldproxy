/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatXml implements CollectionsFormatExtension, CommonFormatExtension {

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    private final GmlConfig gmlConfig;

    @ServiceController(value = false)
    private boolean enable;

    public Wfs3OutputFormatXml(@Requires GmlConfig gmlConfig) {
        this.gmlConfig = gmlConfig;
    }

    @Validate
    private void onStart() {
        this.enable = gmlConfig.isEnabled();
    }

    @Override
    public String getPathPattern() {
        return "^\\/?(?:conformance|collections(/[\\w\\-]+)?)?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GmlConfiguration.class);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage, OgcApiApi api, OgcApiRequestContext requestContext) {
        String title = requestContext.getApi().getData().getLabel();
        String description = requestContext.getApi().getData().getDescription().orElse(null);
        return new LandingPageXml(apiLandingPage.getLinks(), title, description);
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiApi api, OgcApiRequestContext requestContext) {
        return new Wfs3ConformanceClassesXml(conformanceDeclaration);
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApiApi api, OgcApiRequestContext requestContext) {
        return new Wfs3CollectionsXml(collections);
    }

    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                          OgcApiApi api, OgcApiRequestContext requestContext) {
        return new Wfs3CollectionsXml(new ImmutableCollections.Builder()
                .addCollections(ogcApiCollection)
                .build());
    }
}
