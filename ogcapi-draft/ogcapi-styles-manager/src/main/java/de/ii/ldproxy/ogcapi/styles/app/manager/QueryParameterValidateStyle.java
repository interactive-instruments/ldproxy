/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterValidateStyle extends ApiExtensionCache implements OgcApiQueryParameter {

    private Schema schema = new StringSchema()._enum(ImmutableList.of("yes","no","only"))._default("no");

    @Override
    public String getId() {
        return "validateStyle";
    }

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getDescription() {
        return "'yes' creates or updates a style after successful validation and returns 400," +
                "if validation fails. 'no' creates or updates the style without validation. 'only' just " +
                "validates the style without creating a new style or updating an existing style " +
                "and returns 400, if validation fails, otherwise 204.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               ((method== HttpMethods.PUT && definitionPath.equals("/styles/{styleId}")) ||
                (method== HttpMethods.POST && definitionPath.equals("/styles"))));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class, StylesConfiguration::getManagerEnabled);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

}
