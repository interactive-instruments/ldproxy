/**
 * Copyright 2020 interactive instruments GmbH
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zahnen
 */
public class JsonNestingTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNestingTracker.class);

    private List<String> lastPath = new ArrayList<>();
    private Map<String, Integer> lastMultiplicityLevels = new HashMap<>();
    private int pathDiffersAt;
    private int multiplicityDiffersAt;
    private List<String> currentCloseActions;
    private List<List<String>> currentOpenActions;

    //TODO provide JsonNestingStrategy, use for open and close
    private final JsonNestingStrategy nestingStrategy;

    public JsonNestingTracker(JsonNestingStrategy nestingStrategy) {
        this.nestingStrategy = nestingStrategy;
    }

    public void track(List<String> path, List<Integer> multiplicities, JsonGenerator json, boolean doNotCloseValueArray) throws IOException {
        this.pathDiffersAt = getPathDiffIndex(path, lastPath);
        Map<String, Integer> nextMultiplicityLevels =  getMultiplicityLevels(path, multiplicities, lastMultiplicityLevels);
        this.multiplicityDiffersAt = getMultiplicityDiffIndex(path, nextMultiplicityLevels, lastMultiplicityLevels);

        boolean inArray = false;
        if (multiplicityDiffersAt > -1) {
            String element = path.get(multiplicityDiffersAt);
            String multiplicityKey = hasMultiplicity(element) ? getMultiplicityKey(element) : element;
            inArray = differsAt() == multiplicityDiffersAt && nextMultiplicityLevels.getOrDefault(multiplicityKey, 1) > 1;
        }

        this.currentCloseActions = getCloseActions(lastPath, differsAt(), inArray, json, doNotCloseValueArray);
        this.currentOpenActions = getOpenActions(path, differsAt(), inArray, json);

        this.lastMultiplicityLevels = nextMultiplicityLevels;
        this.lastPath = path;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("TRACKER {} {} {} {} {} {}", path, pathDiffersAt, multiplicityDiffersAt, inArray, currentOpenActions, currentCloseActions);
        }
    }

    public int differsAt() {
        return multiplicityDiffersAt == -1 ? pathDiffersAt : Math.min(pathDiffersAt, multiplicityDiffersAt);
    }

    public List<String> getCurrentCloseActions() {
        return currentCloseActions;
    }

    public List<List<String>> getCurrentOpenActions() {
        return currentOpenActions;
    }

    public int getCurrentMultiplicityLevel(String multiplicityKey) {
        return lastMultiplicityLevels.getOrDefault(multiplicityKey, 1);
    }

    private List<String> getCloseActions(List<String> previousPath, int nextPathDiffersAt, boolean inArray, JsonGenerator json, boolean doNotCloseValueArray) throws IOException {
        List<String> actions = new ArrayList<>();

        for (int i = previousPath.size()-1; i >= nextPathDiffersAt; i--) {
            String element = previousPath.get(i);

            boolean closeObject = i < previousPath.size() - 1;
            // omit when value array (end of path array)
            boolean inValueArray = inArray && previousPath.size() -1 == nextPathDiffersAt;
            //omit when already inside of object array
            boolean inObjectArray = closeObject && inArray && i == nextPathDiffersAt;
            boolean closeArray = hasMultiplicity(element) && !inValueArray && !inObjectArray && !(doNotCloseValueArray && i == previousPath.size()-1);//(closeObject && i == nextPathDiffersAt && i > 0);

            if (closeObject) {
                nestingStrategy.closeObject(json);
                actions.add("OBJECT");
            }
            if (closeArray) {
                nestingStrategy.closeArray(json);
                actions.add("ARRAY");
            }
        }

        return actions;
    }

    private List<List<String>> getOpenActions(List<String> nextPath, int nextPathDiffersAt, boolean inArray, JsonGenerator json) throws IOException {
        List<List<String>> actions = new ArrayList<>();

        for (int i = nextPathDiffersAt; i < nextPath.size(); i++) {
            String element = nextPath.get(i);

            boolean openObject = i < nextPath.size() - 1;
            //omit when already inside of object array
            boolean inObjectArray = openObject && inArray && i == nextPathDiffersAt;
            boolean openArray = hasMultiplicity(element) && !inArray && !inObjectArray;
            boolean openField = !openObject &&!openArray && (!inArray || i > nextPathDiffersAt);

            List<String> a = new ArrayList<>();

            if (openArray) {
                if (hasMultiplicity(element)) {
                    // array field
                    nestingStrategy.openArray(json, getFieldName(element));
                } else {
                    //TODO ever used?
                    nestingStrategy.openArray(json);
                }
                a.add("ARRAY");
            }
            if (openObject) {
                if (hasMultiplicity(element)) {
                    // in object array
                    nestingStrategy.openObjectInArray(json, getFieldName(element));
                } else {
                    // object field
                    nestingStrategy.openObject(json, element);
                }
                a.add("OBJECT");
            }
            if (openField) {
                nestingStrategy.openField(json, element);
                a.add("VALUE");
            }

            actions.add(a);
        }

        nestingStrategy.open(json, nextPathDiffersAt);

        return actions;
    }

    private int getPathDiffIndex(List<String> path, List<String> path2) {
        // find index where path2 and path start to differ
        int i;
        for (i = 0; i < path2.size() && i < path.size(); i++) {
            if (!Objects.equals(path2.get(i), path.get(i))) break;
        }
        return i;
    }

    private Map<String, Integer> getMultiplicityLevels(List<String> path, List<Integer> multiplicities, Map<String, Integer> previousMultiplicityLevels) {
        final int[] current = {0};
        final Map<String, Integer> nextMultiplicityLevels = new HashMap<>(previousMultiplicityLevels);

        path.stream()
            .filter(this::hasMultiplicity)
            .map(this::getMultiplicityKey)
            .forEach(multiplicityKey -> {

                int currentMultiplicityLevel = multiplicities.size() > current[0] ? multiplicities.get(current[0]) : 1;
                nextMultiplicityLevels.putIfAbsent(multiplicityKey, currentMultiplicityLevel);
                int lastMultiplicityLevel = previousMultiplicityLevels.getOrDefault(multiplicityKey, 1);

                if(LOGGER.isTraceEnabled())
                LOGGER.trace("{} {} {}", multiplicityKey, currentMultiplicityLevel, lastMultiplicityLevel);

                if (!Objects.equals(lastMultiplicityLevel, currentMultiplicityLevel)) {
                    nextMultiplicityLevels.put(multiplicityKey, currentMultiplicityLevel);
                }

                current[0]++;
            });

        return nextMultiplicityLevels;
    }



    private int getMultiplicityDiffIndex(List<String> path, Map<String, Integer> nextMultiplicityLevels, Map<String, Integer> previousMultiplicityLevels) {
        int currentIndex = 0;

        for (String element : path) {
            if (hasMultiplicity(element)) {
                String multiplicityKey = getMultiplicityKey(element);
                if (!Objects.equals(nextMultiplicityLevels.get(multiplicityKey), previousMultiplicityLevels.get(multiplicityKey))) {
                    return currentIndex;
                }

                currentIndex++;
            }
        }

        return -1;
    }

    private boolean hasMultiplicity(String pathElement) {
        return pathElement.contains("[");
    }

    private String getMultiplicityKey(String pathElement) {
        String multiplicityKey = pathElement.substring(pathElement.indexOf("[") + 1, pathElement.indexOf("]"));

        return multiplicityKey.isEmpty() ? pathElement.substring(0, pathElement.indexOf("[")) : multiplicityKey;
    }

    private String getFieldName(String pathElement) {
        return pathElement.substring(0, pathElement.indexOf("["));
    }
}
