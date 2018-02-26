/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface CoordinateFormatter {
    void open() throws IOException;
    void close() throws IOException;
    void separator() throws IOException;
    void value(String value) throws IOException;
    void value(char[] chars, int i, int j) throws IOException;
    void raw(char[] chars, int i, int j) throws IOException;
}
