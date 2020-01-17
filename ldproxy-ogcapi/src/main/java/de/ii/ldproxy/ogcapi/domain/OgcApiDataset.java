/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.service.api.Service;

import java.util.List;
import java.util.Optional;


public interface OgcApiDataset extends Service {

    // TODO: move the following 3 methods to OgcApiApi, split generic parts of OgcApiDatasetData to OgcApiApiData (requires a change in xtraplatform)
    @Override
    OgcApiDatasetData getData();

    <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType,
                                                            String path);

    <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, OgcApiMediaType mediaType,
                                                            String path, Optional<T> excludeFormat);
}
