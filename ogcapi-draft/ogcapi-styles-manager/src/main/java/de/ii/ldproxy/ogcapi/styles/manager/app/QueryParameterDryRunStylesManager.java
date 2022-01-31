/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager.app;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterDryRunStylesManager extends ApiExtensionCache implements OgcApiQueryParameter {

    private final Schema schema = new BooleanSchema()._default(false);

    @Override
    public String getId() {
        return "dryRunStylesManager";
    }

    @Override
    public String getName() {
        return "dry-run";
    }

    @Override
    public String getDescription() {
        return "'true' just validates the style without creating a new style or updating an existing style " +
                "and returns 400, if validation fails, otherwise 204.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
                isEnabledForApi(apiData) &&
                        ((method==HttpMethods.PUT && definitionPath.endsWith("/styles/{styleId}/metadata")) ||
                                (method==HttpMethods.PATCH && definitionPath.endsWith("/styles/{styleId}/metadata")) ||
                                (method==HttpMethods.PUT && definitionPath.endsWith("/styles/{styleId}")) ||
                                (method==HttpMethods.POST && definitionPath.endsWith("/styles"))));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class, StylesConfiguration::getManagerEnabled) &&
               isExtensionEnabled(apiData, StylesConfiguration.class, StylesConfiguration::getValidationEnabled);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

}
