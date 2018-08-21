/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJsonTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJsonTest.class);

    @Test(groups = {"default"})
    public void testNestingOneLevel() throws IOException {
        // nested property is followed by a flat one
        assertEquals(getActualNestingOneLevel(false), getExpectedNestingOneLevel(false));

        // nested property comes last
        assertEquals(getActualNestingOneLevel(true), getExpectedNestingOneLevel(true));

        // nested and multiple property is followed by a flat one
        assertEquals(getActualNestingOneLevelMultiplicity(false), getExpectedNestingOneLevelMultiplicity(false));

        // nested and multiple property comes last
        assertEquals(getActualNestingOneLevelMultiplicity(true), getExpectedNestingOneLevelMultiplicity(true));
    }

    @Test(groups = {"default"})
    public void testNestingTwoLevel() throws IOException {
        // nested and multiple property is followed by a flat one
        assertEquals(getActualNestingTwoLevelMultiplicity(false), getExpectedNestingTwoLevelMultiplicity(false));
    }

    private String getActualNestingOneLevel(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(json, false, null, new ArrayList<>(), 10, "");
        GeoJsonPropertyMapping mapping = new GeoJsonPropertyMapping();

        json.writeStartObject();
        json.writeObjectFieldStart("properties");

        mapping.setName("foto.bemerkung");
        transformer.onPropertyStart(mapping, ImmutableList.of());
        transformer.onPropertyEnd();

        mapping.setName("foto.hauptfoto");
        transformer.onPropertyStart(mapping, ImmutableList.of());
        transformer.onPropertyEnd();

        if (lastPropertyIsNested) {

        } else {
            mapping.setName("kennung");
            transformer.onPropertyStart(mapping, ImmutableList.of());
            transformer.onPropertyEnd();
        }

        transformer.onFeatureEnd();

        String actual = json.toString();
        LOGGER.debug("ACTUAL   {}", actual);
        return actual;
    }

    private String getExpectedNestingOneLevel(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        json.writeStartObject();
        json.writeObjectFieldStart("properties");
        json.writeFieldName("foto");
        json.writeStartObject();
        json.writeStringField("bemerkung", "");
        json.writeStringField("hauptfoto", "");
        json.writeEndObject();
        if (!lastPropertyIsNested) {
            json.writeStringField("kennung", "");
        }
        json.writeEndObject();
        json.writeEndObject();

        String expected = json.toString();
        LOGGER.debug("EXPECTED {}", expected);
        return expected;
    }

    private String getActualNestingOneLevelMultiplicity(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(json, false, null, new ArrayList<>(), 10, "");
        GeoJsonPropertyMapping mapping = new GeoJsonPropertyMapping();

        json.writeStartObject();
        json.writeObjectFieldStart("properties");

        // multiple object
        mapping.setName("foto[foto].bemerkung");
        transformer.onPropertyStart(mapping, ImmutableList.of(1));
        transformer.onPropertyEnd();

        mapping.setName("foto[foto].hauptfoto");
        transformer.onPropertyStart(mapping, ImmutableList.of(1));
        transformer.onPropertyEnd();

        mapping.setName("foto[foto].bemerkung");
        transformer.onPropertyStart(mapping, ImmutableList.of(2));
        transformer.onPropertyEnd();

        mapping.setName("foto[foto].hauptfoto");
        transformer.onPropertyStart(mapping, ImmutableList.of(2));
        transformer.onPropertyEnd();

        // multiple value
        mapping.setName("fachreferenz[fachreferenz]");
        transformer.onPropertyStart(mapping, ImmutableList.of(1));
        transformer.onPropertyEnd();

        mapping.setName("fachreferenz[fachreferenz]");
        transformer.onPropertyStart(mapping, ImmutableList.of(2));
        transformer.onPropertyEnd();

        if (!lastPropertyIsNested) {
            mapping.setName("kennung");
            transformer.onPropertyStart(mapping, ImmutableList.of());
            transformer.onPropertyEnd();
        }

        transformer.onFeatureEnd();

        String actual = json.toString();
        LOGGER.debug("ACTUAL   {}", actual);
        return actual;
    }

    private String getExpectedNestingOneLevelMultiplicity(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        json.writeStartObject();
        json.writeObjectFieldStart("properties");
        json.writeFieldName("foto");
        json.writeStartArray();
        json.writeStartObject();
        json.writeStringField("bemerkung", "");
        json.writeStringField("hauptfoto", "");
        json.writeEndObject();
        json.writeStartObject();
        json.writeStringField("bemerkung", "");
        json.writeStringField("hauptfoto", "");
        json.writeEndObject();
        json.writeEndArray();
        json.writeFieldName("fachreferenz");
        json.writeStartArray();
        json.writeString("");
        json.writeString("");
        json.writeEndArray();
        if (!lastPropertyIsNested) {
            json.writeStringField("kennung", "");
        }
        json.writeEndObject();
        json.writeEndObject();

        String expected = json.toString();
        LOGGER.debug("EXPECTED {}", expected);
        return expected;
    }

    private String getActualNestingTwoLevelMultiplicity(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(json, false, null, new ArrayList<>(), 10, "");
        GeoJsonPropertyMapping mapping = new GeoJsonPropertyMapping();

        json.writeStartObject();
        json.writeObjectFieldStart("properties");

        mapping.setName("raumreferenz[raumreferenz].datumAbgleich");
        transformer.onPropertyStart(mapping, ImmutableList.of(1));
        transformer.onPropertyEnd();

        mapping.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].kreis");
        transformer.onPropertyStart(mapping, ImmutableList.of(1, 1));
        transformer.onPropertyEnd();

        mapping.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]");
        transformer.onPropertyStart(mapping, ImmutableList.of(1, 1, 1));
        transformer.onPropertyEnd();

        mapping.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]");
        transformer.onPropertyStart(mapping, ImmutableList.of(1, 1, 2));
        transformer.onPropertyEnd();

        mapping.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].kreis");
        transformer.onPropertyStart(mapping, ImmutableList.of(1, 2));
        transformer.onPropertyEnd();

        mapping.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]");
        transformer.onPropertyStart(mapping, ImmutableList.of(1, 2, 1));
        transformer.onPropertyEnd();

        if (!lastPropertyIsNested) {
            mapping.setName("kennung");
            transformer.onPropertyStart(mapping, ImmutableList.of());
            transformer.onPropertyEnd();
        }

        transformer.onFeatureEnd();

        String actual = json.toString();
        LOGGER.debug("ACTUAL   {}", actual);
        return actual;
    }

    private String getExpectedNestingTwoLevelMultiplicity(boolean lastPropertyIsNested) throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        json.writeStartObject();
        json.writeObjectFieldStart("properties");
        json.writeFieldName("raumreferenz");
        json.writeStartArray();
        json.writeStartObject();
        json.writeStringField("datumAbgleich", "");
        json.writeFieldName("ortsangaben");
        json.writeStartArray();
        json.writeStartObject();
        json.writeStringField("kreis", "");
        json.writeStringField("flurstueckskennung", "");
        json.writeStringField("flurstueckskennung", "");
        json.writeEndObject();
        json.writeStartObject();
        json.writeStringField("kreis", "");
        json.writeStringField("flurstueckskennung", "");
        json.writeEndObject();
        json.writeEndArray();
        json.writeEndObject();
        json.writeEndArray();
        if (!lastPropertyIsNested) {
            json.writeStringField("kennung", "");
        }
        json.writeEndObject();
        json.writeEndObject();

        String expected = json.toString();
        LOGGER.debug("EXPECTED {}", expected);
        return expected;
    }
}