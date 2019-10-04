/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.List;

public interface OgcApiCollectionExtension extends OgcApiContentExtension {

    ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                              FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                              OgcApiDatasetData apiData,
                                              URICustomizer uriCustomizer,
                                              boolean isNested,
                                              OgcApiMediaType mediaType,
                                              List<OgcApiMediaType> alternateMediaTypes);

    default String getResourceName() { return "Collection"; };
}
