/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import de.ii.xtraplatform.kvstore.api.rest.ResourceStore;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface CodelistStore extends ResourceStore<CodelistOld> {

    enum IMPORT_TYPE {
        GML_DICTIONARY
    }

    CodelistOld addCodelist(String id) throws IOException;
    CodelistOld addCodelist(String sourceUrl, IMPORT_TYPE sourceType) throws IOException;
}
