/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.PathParameterType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;

@Component
@Provides
@Instantiate
public class PathParameterTypeSchema extends PathParameterType {

    final static List<String> TYPES = ImmutableList.of("feature", "collection");
    final static String SCHEMA_TYPE_PATTERN = "[\\w\\-]+";

    public PathParameterTypeSchema(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "typeSchema";
    }

    @Override
    protected boolean isApplicablePath(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return TYPES;
    }

    @Override
    public String getPattern() {
        return SCHEMA_TYPE_PATTERN;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
    }

}
