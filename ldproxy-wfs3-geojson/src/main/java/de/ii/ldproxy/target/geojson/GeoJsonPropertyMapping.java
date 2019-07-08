/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.wfs3.api.AbstractWfs3GenericMapping;
import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

/**
 * @author zahnen
 */
public class GeoJsonPropertyMapping extends AbstractWfs3GenericMapping<GeoJsonMapping.GEO_JSON_TYPE> implements GeoJsonMapping {

    private GEO_JSON_TYPE type;

    public GeoJsonPropertyMapping() {
    }

    public GeoJsonPropertyMapping(GeoJsonPropertyMapping mapping) {
        this.enabled = mapping.enabled;
        this.name = mapping.name;
        this.type = mapping.type;
        this.format = mapping.format;
        this.codelist = mapping.codelist;
    }

    @Override
    public GEO_JSON_TYPE getType() {
        return type;
    }

    public void setType(GEO_JSON_TYPE type) {
        this.type = type;
    }

    @Override
    public boolean isSpatial() {
        return getType() == GEO_JSON_TYPE.GEOMETRY;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        GeoJsonPropertyMapping copy = new GeoJsonPropertyMapping(this);
        Wfs3GenericMapping baseMapping = (Wfs3GenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }
        if ((copy.name == null || copy.name.isEmpty()) && baseMapping.getName() != null) {
            copy.name = baseMapping.getName();
        }
        if ((copy.format == null || copy.format.isEmpty()) && baseMapping.getFormat() != null) {
            copy.format = baseMapping.getFormat();
        }
        copy.codelist = baseMapping.getCodelist();

        return copy;
    }
}
