/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

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
public class Wfs3ExtentXml {
    private final OgcApiExtentSpatial spatial;
    private final OgcApiExtentTemporal temporal;

    public Wfs3ExtentXml(OgcApiExtent extent) {
        this.spatial = extent.getSpatial();
        this.temporal = extent.getTemporal();
    }

    @XmlElement(name = "Spatial")
    public Wfs3ExtentSpatialXml getSpatial() {
        return new Wfs3ExtentSpatialXml(String.format(Locale.US, "%f %f", spatial.getBbox()[0][0], spatial.getBbox()[0][1]), String.format(Locale.US, "%f %f", spatial.getBbox()[0][2], spatial.getBbox()[0][3]));
    }

    @XmlElement(name = "Temporal")
    public Wfs3ExtentTemporalXml getTemporal() {
        return Objects.nonNull(temporal) ? new Wfs3ExtentTemporalXml(temporal.getInterval()[0][0], temporal.getInterval()[0][1]) : null;
    }

}
