/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class StylesheetContent {

    final String descriptor;
    final byte[] content;
    final boolean inStore;

    public StylesheetContent(File file) throws IOException {
        content = java.nio.file.Files.readAllBytes(file.toPath());
        descriptor = file.getAbsolutePath();
        inStore = true;
    }

    public StylesheetContent(Path path) throws IOException {
        content = java.nio.file.Files.readAllBytes(path);
        descriptor = path.toFile().getAbsolutePath();
        inStore = true;
    }

    public StylesheetContent(byte[] content, String descriptor, boolean inStore) {
        this.content = content;
        this.descriptor = descriptor;
        this.inStore = inStore;
    }

    public byte[] getContent() {
        return content;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean getInStore() { return inStore; }

}
