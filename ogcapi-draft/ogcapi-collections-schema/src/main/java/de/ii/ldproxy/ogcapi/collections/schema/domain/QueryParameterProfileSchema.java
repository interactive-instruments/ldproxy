/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterProfileSchema extends QueryParameterProfile {

    final static List<String> PROFILES = ImmutableList.of("2019-09", "07");

    @Inject
    public QueryParameterProfileSchema(ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "profileSchema";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
    }

    @Override
    protected List<String> getProfiles(OgcApiDataV2 apiData) {
        return PROFILES;
    }

    @Override
    protected String getDefault(OgcApiDataV2 apiData) {
        return "2019-09";
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
    }

}
