/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class JsonNestingStrategyFlattenSuffix implements JsonNestingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNestingStrategyFlattenSuffix.class);

    private Map<String, Integer> currentMultiplicities = new HashMap<>();
    private final List<String> currentFieldName;
    private final List<String> lastFieldName;
    private final Joiner joiner;

    public JsonNestingStrategyFlattenSuffix(String separator) {
        this.currentFieldName = new ArrayList<>();
        this.lastFieldName = new ArrayList<>();
        this.joiner = Joiner.on(separator)
                            .skipNulls();
    }

    //biotoptyp.1.zusatzbezeichnung.2.zusatzcode.1

    @Override
    public void openField(JsonGenerator json, String key) throws IOException {
        saveFieldName(key);
    }

    @Override
    public void openObjectInArray(JsonGenerator json, String key, boolean firstObject) throws IOException {
        if (!firstObject)
            currentMultiplicities.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }

    @Override
    public void openArray(JsonGenerator json) throws IOException {
        if (!currentFieldName.isEmpty()) {
            currentMultiplicities.compute(currentFieldName.get(currentFieldName.size() - 1), (k, v) -> (v == null) ? 1 : v + 1);
        }
    }

    @Override
    public void openObject(JsonGenerator json, String key) throws IOException {
        saveFieldName(key);
    }

    @Override
    public void openArray(JsonGenerator json, String key) throws IOException {
        saveFieldName(key);
        currentMultiplicities.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }

    @Override
    public void closeObject(JsonGenerator json) throws IOException {
    }

    @Override
    public void closeArray(JsonGenerator json) throws IOException {
    }

    @Override
    public void open(JsonGenerator json, int nextPathDiffersAt, int nextMultiplicityDiffersAt) throws IOException {

        if (currentFieldName.isEmpty() && lastFieldName.size() - 1 == nextMultiplicityDiffersAt) {
            //value multiplicity change
            currentMultiplicities.compute(lastFieldName.get(lastFieldName.size() - 1), (k, v) -> (v == null) ? 1 : v + 1);
        } else {
            //reset multiplicities for closed elements
            for (int i = nextPathDiffersAt; i < lastFieldName.size(); i++) {
                currentMultiplicities.remove(lastFieldName.get(i));
            }
        }

        writeFieldName(json, nextPathDiffersAt, nextMultiplicityDiffersAt);

        lastFieldName.clear();
        lastFieldName.addAll(currentFieldName);
        currentFieldName.clear();
    }

    private void saveFieldName(String element) {
        if (Objects.nonNull(element)) {
            currentFieldName.add(element);
        }
    }

    private void writeFieldName(JsonGenerator json, int nextPathDiffersAt, int nextMultiplicityDiffersAt) throws IOException {
        if (currentFieldName.isEmpty() && lastFieldName.size() - 1 == nextMultiplicityDiffersAt) {
            //value multiplicity change
            currentFieldName.addAll(lastFieldName);
        } else if (!lastFieldName.isEmpty() && lastFieldName.size() > nextPathDiffersAt) {
            // prepend with unchanged elements
            currentFieldName.addAll(0, lastFieldName.subList(0, nextPathDiffersAt));
        }

        json.writeFieldName(joiner.join(currentFieldName.stream()
                                                        .flatMap(element -> {

                                                            if (currentMultiplicities.containsKey(element)) {
                                                                return Stream.of(element, String.valueOf(currentMultiplicities.get(element)));
                                                            }

                                                            return Stream.of(element);
                                                        })
                                                        .collect(Collectors.toList())));
    }
}
