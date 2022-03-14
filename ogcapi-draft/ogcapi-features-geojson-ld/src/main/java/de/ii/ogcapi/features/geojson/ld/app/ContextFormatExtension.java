/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.app;

import de.ii.ogcapi.foundation.domain.FormatExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface ContextFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/?collections/"+COLLECTION_ID_PATTERN+"/context/?$";
    }

    default InputStream getInputStream(Path context) throws IOException {
        return Files.newInputStream(context);
    }
}
