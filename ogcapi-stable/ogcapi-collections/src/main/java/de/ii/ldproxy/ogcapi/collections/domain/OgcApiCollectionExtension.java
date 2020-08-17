/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import de.ii.ldproxy.ogcapi.domain.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface OgcApiCollectionExtension extends OgcApiContentExtension {

    ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                              FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                              OgcApiApiDataV2 apiData,
                                              URICustomizer uriCustomizer,
                                              boolean isNested,
                                              OgcApiMediaType mediaType,
                                              List<OgcApiMediaType> alternateMediaTypes,
                                              Optional<Locale> language);

    default String getResourceName() { return "Collection"; };
}
