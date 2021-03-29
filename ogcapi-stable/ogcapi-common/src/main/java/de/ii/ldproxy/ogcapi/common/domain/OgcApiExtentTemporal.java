/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class OgcApiExtentTemporal {
    private String[][] interval;
    private String trs;

    public OgcApiExtentTemporal(Long begin, Long end) {
        this.interval = new String[][]{{
                (begin!=null) ? Instant.ofEpochMilli(begin).truncatedTo(ChronoUnit.SECONDS).toString() : null,
                (end!=null) ? Instant.ofEpochMilli(end).truncatedTo(ChronoUnit.SECONDS).toString() : null
        }};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public OgcApiExtentTemporal(String begin, String end) {
        this.interval = new String[][]{{begin, end}};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public String[][] getInterval() {
        return interval;
    }

    public void setInterval(String[][] interval) {
        this.interval = interval;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }
}
