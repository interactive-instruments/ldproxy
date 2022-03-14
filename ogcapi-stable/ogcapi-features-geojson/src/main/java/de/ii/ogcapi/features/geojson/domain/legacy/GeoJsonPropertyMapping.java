/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain.legacy;

import de.ii.ogcapi.collections.domain.AbstractOgcApiFeaturesGenericMapping;
import de.ii.ogcapi.collections.domain.legacy.OgcApiFeaturesGenericMapping;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import java.util.List;

/**
 * @author zahnen
 */
@Deprecated
public class GeoJsonPropertyMapping extends AbstractOgcApiFeaturesGenericMapping<GeoJsonMapping.GEO_JSON_TYPE> implements GeoJsonMapping {

    private GEO_JSON_TYPE type;

    private String ldContext;
    private List<String> ldType;
    private boolean idAsProperty;
    private String idTemplate;

    public GeoJsonPropertyMapping() {
    }

    public GeoJsonPropertyMapping(GeoJsonPropertyMapping mapping) {
        this.enabled = mapping.enabled;
        this.name = mapping.name;
        this.type = mapping.type;
        this.format = mapping.format;
        this.codelist = mapping.codelist;
        //TODO
        this.baseMapping = mapping.baseMapping;

        this.ldContext = mapping.ldContext;
        this.ldType = mapping.ldType;
        this.idAsProperty = mapping.idAsProperty;
        this.idTemplate = mapping.idTemplate;
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
        super.mergeCopyWithBase(targetMapping);

        GeoJsonPropertyMapping copy = new GeoJsonPropertyMapping(this);
        OgcApiFeaturesGenericMapping baseMapping = (OgcApiFeaturesGenericMapping) targetMapping;

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

    public String getLdContext() {
        return ldContext;
    }

    public void setLdContext(String ldContext) {
        this.ldContext = ldContext;
    }

    public List<String> getLdType() {
        return ldType;
    }

    public void setLdType(List<String> ldType) {
        this.ldType = ldType;
    }

    public boolean isIdAsProperty() {
        return idAsProperty;
    }

    public void setIdAsProperty(boolean idAsProperty) {
        this.idAsProperty = idAsProperty;
    }

    public String getIdTemplate() {
        return idTemplate;
    }

    public void setIdTemplate(String idTemplate) {
        this.idTemplate = idTemplate;
    }
}
