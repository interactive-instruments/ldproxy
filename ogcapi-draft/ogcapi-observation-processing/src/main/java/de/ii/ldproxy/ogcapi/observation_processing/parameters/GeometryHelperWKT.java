/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import java.util.List;

public interface GeometryHelperWKT {
    String getPointRegex();
    String getLineStringRegex();
    String getPolygonRegex();
    String getMultiPolygonRegex();
    List<Double> extractPosition(String text);
    List<List<Double>> extractLineString(String text);
    List<List<List<Double>>> extractPolygon(String text);
    List<List<List<List<Double>>>> extractMultiPolygon(String text);
    List<List<List<Double>>> convertBboxToPolygon(String bbox);
    List<List<List<Double>>> convertBboxToPolygon(List<Double> bbox);
    String convertMultiPolygonToWkt(List<List<List<List<Double>>>> multiPolygon);
    String convertPolygonToWkt(List<List<List<Double>>> polygon);
    String convertLineStringToWkt(List<List<Double>> lineString);
    String convertPointToWkt(List<Double> point);
}
