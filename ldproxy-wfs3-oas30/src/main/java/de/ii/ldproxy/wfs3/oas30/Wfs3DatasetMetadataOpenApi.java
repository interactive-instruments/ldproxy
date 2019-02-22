/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collections;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3DatasetMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Collection;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3DatasetMetadataOpenApi implements Wfs3DatasetMetadataExtension {
    @Override
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3, Wfs3ServiceData serviceData) {
        if (isExtensionEnabled(serviceData, Oas30Configuration.class)) {

            collections.addSections(ImmutableMap.of("title", "API Definition", "links", ImmutableList.of(ImmutableMap.of("title", "OpenAPI 3.0", "url", uriCustomizer.ensureLastPathSegment("api").toString()))));
        }

        return collections;
    }
}
