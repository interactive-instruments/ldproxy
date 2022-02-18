/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class QueryParameterLang extends ApiExtensionCache implements OgcApiQueryParameter {

    private Schema schema = null;
    private final ExtensionRegistry extensionRegistry;

    @Inject
    public QueryParameterLang(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "lang";
    }

    @Override
    public String getDescription() {
        return "Select the language of the response. If no value is provided, " +
                "the standard HTTP rules apply, i.e., the accept-lang header will be used to determine the format.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET);
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (schema==null) {
            schema = new StringSchema()._enum(I18n.getLanguages().stream().map(lang -> lang.getLanguage()).collect(Collectors.toList()));
        }
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FoundationConfiguration.class, FoundationConfiguration::getUseLangParameter);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FoundationConfiguration.class;
    }
}
