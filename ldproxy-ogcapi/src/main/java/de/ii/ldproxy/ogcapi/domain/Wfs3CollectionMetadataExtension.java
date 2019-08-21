/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

/**
 * @author zahnen
 */
public interface Wfs3CollectionMetadataExtension extends OgcApiExtension {

    ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationOgcApi featureTypeConfiguration, URICustomizer uriCustomizer, boolean isNested, OgcApiDatasetData datasetData);
}
