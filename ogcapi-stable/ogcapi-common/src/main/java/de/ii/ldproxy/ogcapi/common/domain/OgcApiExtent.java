/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class OgcApiExtent {
    private Optional<OgcApiExtentSpatial> spatial;
    private Optional<OgcApiExtentTemporal> temporal;

    public OgcApiExtent() {
        this.spatial = Optional.empty();
        this.temporal = Optional.empty();
    }

    public OgcApiExtent(String xmin, String ymin, String xmax, String ymax) {
        this.spatial = Optional.of(new OgcApiExtentSpatial(xmin, ymin, xmax, ymax));
        this.temporal = Optional.empty();
    }

    public OgcApiExtent(double xmin, double ymin, double xmax, double ymax) {
        this.spatial = Optional.of(new OgcApiExtentSpatial(xmin, ymin, xmax, ymax));
        this.temporal = Optional.empty();
    }

    public OgcApiExtent(String begin, String end) {
        this.spatial = Optional.empty();
        this.temporal = Optional.of(new OgcApiExtentTemporal(begin, end));
    }

    public OgcApiExtent(Long begin, Long end) {
        this.spatial = Optional.empty();
        this.temporal = Optional.of(new OgcApiExtentTemporal(begin, end));
    }

    public OgcApiExtent(Long begin, Long end, double xmin, double ymin, double xmax, double ymax) {
        this.spatial = Optional.of(new OgcApiExtentSpatial(xmin, ymin, xmax, ymax));
        this.temporal = Optional.of(new OgcApiExtentTemporal(begin, end));
    }

    public OgcApiExtent(String begin, String end, String xmin, String ymin, String xmax, String ymax) {
        this.spatial = Optional.of(new OgcApiExtentSpatial(xmin, ymin, xmax, ymax));
        this.temporal = Optional.of(new OgcApiExtentTemporal(begin, end));
    }
    public Optional<OgcApiExtentSpatial> getSpatial() {
        return spatial;
    }

    public void setSpatial(OgcApiExtentSpatial spatial) {
        this.spatial = Optional.ofNullable(spatial);
    }

    public Optional<OgcApiExtentTemporal> getTemporal() {
        return temporal;
    }

    public void setTemporal(OgcApiExtentTemporal temporal) {
        this.temporal = Optional.ofNullable(temporal);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcApiExtent> FUNNEL = (from, into) -> {
        from.getSpatial().ifPresent(val -> OgcApiExtentSpatial.FUNNEL.funnel(val, into));
        from.getTemporal().ifPresent(val -> OgcApiExtentTemporal.FUNNEL.funnel(val, into));
    };

}
