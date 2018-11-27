/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import de.ii.xtraplatform.crs.api.CoordinateFormatter;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class JtsCoordinatesWriter implements CoordinateFormatter {
    private final List<Coordinate> coordinates;
    private int count;
    private final double[] values;

    public JtsCoordinatesWriter() {
        this.coordinates = new ArrayList<>();
        this.values = new double[2];
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    @Override
    public void open() throws IOException {

    }

    @Override
    public void close() throws IOException {
        this.count = 0;

        coordinates.add(new Coordinate(values[0], values[1]));
    }

    @Override
    public void separator() throws IOException {
        this.count = 0;

        coordinates.add(new Coordinate(values[0], values[1]));
    }

    @Override
    public void value(String s) throws IOException {
        values[count++] = Double.valueOf(s);
    }

    @Override
    public void value(char[] chars, int i, int i1) throws IOException {
        values[count++] = Double.valueOf(String.copyValueOf(chars,i,i1));
    }

    @Override
    public void raw(char[] chars, int i, int i1) throws IOException {
        values[count++] = Double.valueOf(String.copyValueOf(chars,i,i1));
    }
}
