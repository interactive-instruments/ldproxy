/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class OgcApiQueryablesHtml implements OgcApiQueryablesFormatExtension {

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return getExtensionConfiguration(apiData, QueryablesConfiguration.class)
                .map(QueryablesConfiguration::getEnabled)
                .orElse(false);
    }

    @Override
    public Response getResponse(Queryables queryables, String collectionId, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return Response.ok()
                .entity(queryables)
                .build();
    }
}
