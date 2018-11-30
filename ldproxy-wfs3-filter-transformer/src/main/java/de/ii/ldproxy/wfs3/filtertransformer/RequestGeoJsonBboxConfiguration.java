/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableRequestGeoJsonBboxConfiguration.class)
public abstract class RequestGeoJsonBboxConfiguration implements FilterTransformerConfiguration {

    public static final String TRANSFORMER_TYPE = "REQUEST_GEOJSON_BBOX";

    public abstract String getId();

    public abstract String getLabel();

    public abstract String getUrlTemplate();

    public abstract List<String> getParameters();
}
