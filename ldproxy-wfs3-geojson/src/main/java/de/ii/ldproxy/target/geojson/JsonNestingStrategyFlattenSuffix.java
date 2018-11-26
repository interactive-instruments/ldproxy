/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class JsonNestingStrategyFlattenSuffix implements JsonNestingStrategy {

    private List<String> currentPath = new ArrayList<>();
    private List<Integer> currentArraySuffix = new ArrayList<>();

    //biotoptyp.1.zusatzbezeichnung.2.zusatzcode.1

    @Override
    public void openObjectInArray(JsonGenerator json, String key) throws IOException {
        json.writeStartObject();
        Integer count = currentArraySuffix.remove(currentArraySuffix.size() - 1);
        currentArraySuffix.add(count+1);
    }

    @Override
    public void openArray(JsonGenerator json) throws IOException {
        json.writeStartArray();
    }

    @Override
    public void openObject(JsonGenerator json, String key) throws IOException {
        json.writeObjectFieldStart(key);
    }

    @Override
    public void openArray(JsonGenerator json, String key) throws IOException {
        json.writeArrayFieldStart(key);
        currentPath.add(key);
        currentArraySuffix.add(0);
    }

    @Override
    public void closeObject(JsonGenerator json) throws IOException {
        json.writeEndObject();
    }

    @Override
    public void closeArray(JsonGenerator json) throws IOException {
        json.writeEndArray();
    }

    @Override
    public void open(JsonGenerator json) throws IOException {

    }
}
