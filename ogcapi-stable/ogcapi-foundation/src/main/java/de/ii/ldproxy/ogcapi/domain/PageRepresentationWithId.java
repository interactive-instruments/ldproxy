/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.hash.Funnel;

import java.nio.charset.StandardCharsets;

public abstract class PageRepresentationWithId extends PageRepresentation {

    public abstract String getId();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<PageRepresentationWithId> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
         into.putString(from.getId(), StandardCharsets.UTF_8);
    };
}
