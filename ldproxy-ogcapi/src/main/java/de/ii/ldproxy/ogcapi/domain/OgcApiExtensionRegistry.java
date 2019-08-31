/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.List;

/**
 * @author zahnen
 */
public interface OgcApiExtensionRegistry {

    // TODO: temporary hack so that the ogcapi-features-1/core conformance class can be added, too. Refactoring is required so that the extension registry is not part of Wfs3Core
    void addExtension(OgcApiExtension extension);

    List<OgcApiExtension> getExtensions();

    <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType);
}
