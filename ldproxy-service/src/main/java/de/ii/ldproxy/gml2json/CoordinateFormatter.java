/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
