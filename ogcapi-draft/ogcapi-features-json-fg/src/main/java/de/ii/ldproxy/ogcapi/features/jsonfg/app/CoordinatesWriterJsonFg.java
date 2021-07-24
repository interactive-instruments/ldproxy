/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class CoordinatesWriterJsonFg implements CoordinatesWriter<JsonGenerator> {

    // TODO this class is necessary, because CoordinatesWriterJson writes the numbers as Raw which results in an exception (embedded string object);
    //      maybe use writeNumber also in CoordinatesWriterJson?

    @Override
    public void onStart() throws IOException {
        getDelegate().writeStartArray();
    }

    @Override
    public void onSeparator() throws IOException {
        getDelegate().writeEndArray();
        getDelegate().writeStartArray();
    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeNumber(new String(chars, offset, length));
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeNumber(new String(chars, offset, length));
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
        getDelegate().writeNumber(new String(chars, offset, length));
    }

    @Override
    public void onEnd() throws IOException {
        getDelegate().writeEndArray();
    }
}
