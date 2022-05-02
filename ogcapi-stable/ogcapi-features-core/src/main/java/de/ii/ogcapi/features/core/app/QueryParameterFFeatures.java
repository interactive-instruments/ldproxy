/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.QueryParameterFSubCollection;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterFFeatures extends QueryParameterFSubCollection {

    @Inject
    public QueryParameterFFeatures(ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
        super(extensionRegistry, schemaValidator);
    }

    @Override
    public String getId() {
        return "fFeatures";
    }

    @Override
    protected boolean matchesPath(String definitionPath) {
        return definitionPath.startsWith("/collections/{collectionId}/items");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return FeatureFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

}
