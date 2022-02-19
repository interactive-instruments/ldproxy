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
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.List;

@Singleton
@AutoBind
public class PathParameterTypeSchema extends PathParameterType {

    final static List<String> TYPES = ImmutableList.of("feature", "collection");
    final static String SCHEMA_TYPE_PATTERN = "[\\w\\-]+";

    @Inject
    public PathParameterTypeSchema(ExtensionRegistry extensionRegistry) {
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
