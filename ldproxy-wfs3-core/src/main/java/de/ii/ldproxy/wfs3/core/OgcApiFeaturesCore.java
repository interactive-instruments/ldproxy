/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides(specifications = {OgcApiFeaturesCore.class, ConformanceClass.class, OgcApiExtension.class})
@Instantiate
public class OgcApiFeaturesCore implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiFeaturesCore.class);

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    /* TODO
    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiFeaturesCore(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

    }

    public static void checkCollectionId(OgcApiDatasetData datasetData, String collectionName) {
        if (!datasetData.isFeatureTypeEnabled(collectionName)) {
            throw new NotFoundException();
        }
    }

    private List<OgcApiCollectionExtension> getCollectionExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiCollectionExtension.class);
    }

     */

}
