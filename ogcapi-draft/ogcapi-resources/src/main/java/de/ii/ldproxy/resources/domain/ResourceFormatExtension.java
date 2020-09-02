/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.domain;

import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;

public interface ResourceFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/resources/[^/]+/?$";
    }

    @Override
    default boolean canSupportTransactions() {
        return true;
    }

    Response getResourceResponse(byte[] resource,
                                 String resourceId,
                                 OgcApi api,
                                 ApiRequestContext requestContext);

    Response putResource(Path resourcesStore,
                         byte[] resource,
                         String resourceId,
                         OgcApi api,
                         ApiRequestContext requestContext) throws IOException;

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }
}
