/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.CommonConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Provides
@Instantiate
public class PathParameterResourceApiDefinition implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterResourceApiDefinition.class);
    Map<String,Set<String>> apiCollectionMap;

    public PathParameterResourceApiDefinition() {
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public String getPattern() {
        return "[^/]+";
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return ImmutableList.of();
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema();
    }

    @Override
    public String getName() {
        return "resource";
    }

    @Override
    public String getDescription() {
        return "The filename of a file referenced from an API definition.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                definitionPath.equals("/api/{resource}");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }
}
