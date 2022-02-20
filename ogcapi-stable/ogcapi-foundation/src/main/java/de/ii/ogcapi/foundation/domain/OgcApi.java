/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.services.domain.Service;

import java.util.List;
import java.util.Optional;


public interface OgcApi extends Service {

    @Override
    OgcApiDataV2 getData();

    <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, ApiMediaType mediaType,
                                                            String path, Optional<String> collectionId);

    <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, ApiMediaType mediaType,
                                                            String path, Optional<T> excludeFormat);
}
