/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonDeserialize(builder = ImmutableApiCatalog.Builder.class)
public abstract class ApiCatalog extends PageRepresentation {

    public abstract URI getCatalogUri();

    public abstract List<ApiCatalogEntry> getApis();

    @JsonIgnore
    public abstract String getUrlPrefix();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
