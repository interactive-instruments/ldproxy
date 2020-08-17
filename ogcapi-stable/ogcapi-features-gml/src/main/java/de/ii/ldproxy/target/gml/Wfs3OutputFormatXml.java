/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

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
