/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.auth.api.User;

import javax.ws.rs.NotAuthorizedException;
import java.util.Optional;


public interface OgcApiEndpointExtension extends OgcApiExtension {

    OgcApiContext getApiContext();

    ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 apiData, String subPath);

    default ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        boolean useLangParameter = getExtensionConfiguration(apiData, OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getUseLangParameter)
                .orElse(false);
        if (!useLangParameter)
            return ImmutableSet.of("f");

        return ImmutableSet.of("f", "lang");
    }

    default void checkAuthorization(OgcApiApiDataV2 apiData, Optional<User> optionalUser) {
        if (apiData.getSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }

    default boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}
