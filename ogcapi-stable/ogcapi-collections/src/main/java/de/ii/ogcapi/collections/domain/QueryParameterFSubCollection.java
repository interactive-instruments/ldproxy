/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;

import java.util.Objects;

/**
 * @langEn Todo
 * @langDe Todo
 * @name Sub Collection
 * @endpoints Todo
 */

public abstract class QueryParameterFSubCollection extends QueryParameterF {

    public QueryParameterFSubCollection(ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return matchesPath(definitionPath) &&
            isEnabledForApi(apiData, collectionId) &&
            (method.equals(HttpMethods.GET) || method.equals(HttpMethods.HEAD));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getCollections()
            .values()
            .stream()
            .filter(FeatureTypeConfigurationOgcApi::getEnabled)
            .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        return super.isEnabledForApi(apiData, collectionId) &&
            Objects.nonNull(collectionData) &&
            collectionData.getEnabled();
    }
}
