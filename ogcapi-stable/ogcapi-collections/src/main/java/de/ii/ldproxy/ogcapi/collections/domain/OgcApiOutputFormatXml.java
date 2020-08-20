/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import de.ii.ldproxy.ogcapi.domain.*;
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
public class OgcApiOutputFormatXml implements CollectionsFormatExtension {

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    @Override
    public String getPathPattern() {
        return "^\\/collections(?:/[\\w\\-]+)?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledByDefault() { return false; }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApiApi api, OgcApiRequestContext requestContext) {
        return new OgcApiCollectionsXml(collections);
    }

    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                          OgcApiApi api, OgcApiRequestContext requestContext) {
        return new OgcApiCollectionsXml(new ImmutableCollections.Builder()
                .addCollections(ogcApiCollection)
                .build());
    }
}
