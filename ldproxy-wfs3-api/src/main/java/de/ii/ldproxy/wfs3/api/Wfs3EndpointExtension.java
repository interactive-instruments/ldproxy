/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.auth.api.User;

import javax.ws.rs.NotAuthorizedException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface Wfs3EndpointExtension extends Wfs3Extension {

    String getPath();

    default String getSubPathRegex() {
        return null;
    }

    default List<String> getMethods() {
        return ImmutableList.of();
    }

    default boolean matches(String firstPathSegment, String method, String subPath) {
        boolean regex = Objects.isNull(getSubPathRegex());
        if (!regex) {
            regex = subPath.matches(getSubPathRegex());
        }

        return Objects.nonNull(firstPathSegment) && firstPathSegment.startsWith(getPath())
                && (getMethods().isEmpty() || getMethods().contains(method))
                && (regex);
    }

    default void checkAuthorization(Wfs3ServiceData serviceData, Optional<User> optionalUser) {
        if (serviceData.isSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }

    default boolean isEnabledForService(Wfs3ServiceData serviceData){return true;}
}
