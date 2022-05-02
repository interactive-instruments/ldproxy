/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.app;

import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.features.geojson.ld.domain.GeoJsonLdConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.SchemaValidator;

@Singleton
@AutoBind
public class PathParameterCollectionIdGeoJsonLd extends AbstractPathParameterCollectionId {

    @Inject
    PathParameterCollectionIdGeoJsonLd(SchemaValidator schemaValidator) {
      super(schemaValidator);
    }

    @Override
    public String getId() {
        return "collectionIdJsonLdContext";
    }

    @Override
    public boolean matchesPath(String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/context");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonLdConfiguration.class;
    }
}
