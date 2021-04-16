/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app.xml;

import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
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
public class CollectionsFormatXml implements CollectionsFormatExtension {

    private static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledByDefault() { return false; }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApi api, ApiRequestContext requestContext) {
        return new OgcApiCollectionsXml(collections);
    }

    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                      OgcApi api, ApiRequestContext requestContext) {
        return new OgcApiCollectionsXml(new ImmutableCollections.Builder()
                .addCollections(ogcApiCollection)
                .build());
    }
}
