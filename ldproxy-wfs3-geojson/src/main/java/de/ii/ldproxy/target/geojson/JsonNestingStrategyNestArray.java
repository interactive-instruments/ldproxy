/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author zahnen
 */
public class JsonNestingStrategyNestArray implements JsonNestingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNestingStrategyNestArray.class);

    @Override
    public void openObjectInArray(JsonGenerator json, String key) throws IOException {
        json.writeStartObject();
        LOGGER.debug("OPEN OBJECT IN ARRAY");
    }

    @Override
    public void openArray(JsonGenerator json) throws IOException {
        json.writeStartArray();
        LOGGER.debug("OPEN ARRAY");
    }

    @Override
    public void openObject(JsonGenerator json, String key) throws IOException {
        json.writeObjectFieldStart(key);
        LOGGER.debug("OPEN OBJECT {}", key);
    }

    @Override
    public void openArray(JsonGenerator json, String key) throws IOException {
        json.writeArrayFieldStart(key);
        LOGGER.debug("OPEN ARRAY {}", key);
    }

    @Override
    public void closeObject(JsonGenerator json) throws IOException {
        json.writeEndObject();
        LOGGER.debug("CLOSE OBJECT");
    }

    @Override
    public void closeArray(JsonGenerator json) throws IOException {
        json.writeEndArray();
        LOGGER.debug("CLOSE ARRAY");
    }
}
