/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.List;


public interface OgcApiExtensionRegistry {

    List<OgcApiExtension> getExtensions();

    <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType);

    /* TODO delete
    default <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType, OgcApiDatasetData apiData, String path) {
        return getExtensionsForType(extensionType)
                .stream()
                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                .filter(outputFormatExtension -> mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .findFirst();
    }
     */
}
