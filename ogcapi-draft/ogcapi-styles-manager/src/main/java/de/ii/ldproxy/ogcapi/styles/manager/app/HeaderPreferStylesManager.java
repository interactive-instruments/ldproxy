/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiHeader;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class HeaderPreferStylesManager extends ApiExtensionCache implements ApiHeader {

    private final Schema schema = new StringSchema()._enum(ImmutableList.of("handling=strict", "handling=lenient"));

    @Inject
    HeaderPreferStylesManager() {
    }

    @Override
    public String getId() {
        return "Prefer";
    }

    @Override
    public String getDescription() {
        return "'handling=strict' creates or updates a style after successful validation and returns 400, " +
                "if validation fails. 'handling=lenient' (the default) creates or updates the style without validation.";
    }

    @Override
    public boolean isRequestHeader() { return true; }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) { return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
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
