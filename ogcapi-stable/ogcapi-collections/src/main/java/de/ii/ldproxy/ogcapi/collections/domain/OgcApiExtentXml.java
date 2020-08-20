/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiExtent;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtentSpatial;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtentTemporal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Locale;
import java.util.Objects;

/**
 * @author zahnen
 */
@XmlType(propOrder = {"spatial", "temporal"})
public class OgcApiExtentXml {
    private final OgcApiExtentSpatial spatial;
    private final OgcApiExtentTemporal temporal;

    public OgcApiExtentXml(OgcApiExtent extent) {
        this.spatial = extent.getSpatial().orElse(null);
        this.temporal = extent.getTemporal().orElse(null);
    }

    @XmlElement(name = "Spatial", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public OgcApiExtentSpatialXml getSpatial() {
        return Objects.nonNull(spatial) ?
                new OgcApiExtentSpatialXml(String.format(Locale.US, "%f %f", spatial.getBbox()[0][0], spatial.getBbox()[0][1]), String.format(Locale.US, "%f %f", spatial.getBbox()[0][2], spatial.getBbox()[0][3])) :
                null;
    }

    @XmlElement(name = "Temporal", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public OgcApiExtentTemporalXml getTemporal() {
        return Objects.nonNull(temporal) ?
                new OgcApiExtentTemporalXml(temporal.getInterval()[0][0], temporal.getInterval()[0][1]) :
                null;
    }

}
