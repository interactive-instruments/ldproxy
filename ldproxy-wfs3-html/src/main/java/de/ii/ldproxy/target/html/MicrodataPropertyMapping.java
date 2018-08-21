/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.wfs3.api.AbstractWfs3GenericMapping;
import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

/**
 * @author zahnen
 */
public class MicrodataPropertyMapping extends AbstractWfs3GenericMapping<MicrodataMapping.MICRODATA_TYPE> implements MicrodataMapping {

    private MICRODATA_TYPE type;
    private Boolean showInCollection;
    private String itemType;
    private String itemProp;
    private String sparqlQuery;

    public MicrodataPropertyMapping() {
    }

    public MicrodataPropertyMapping(MicrodataPropertyMapping mapping) {
        this.enabled = mapping.enabled;
        this.name = mapping.name;
        this.type = mapping.type;
        this.showInCollection = mapping.showInCollection;
        this.itemType = mapping.itemType;
        this.itemProp = mapping.itemProp;
        this.sparqlQuery = mapping.sparqlQuery;
        this.format = mapping.format;
        this.codelist = mapping.codelist;
    }

    @Override
    public MICRODATA_TYPE getType() {
        return type;
    }

    public void setType(MICRODATA_TYPE type) {
        this.type = type;
    }

    @Override
    public Boolean isShowInCollection() {
        return showInCollection;
    }

    public void setShowInCollection(boolean showInCollection) {
        this.showInCollection = showInCollection;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public String getItemProp() {
        return itemProp;
    }

    @Override
    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public void setItemProp(String itemProp) {
        this.itemProp = itemProp;
    }

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    @Override
    public boolean isSpatial() {
        return getType() == MICRODATA_TYPE.GEOMETRY;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        MicrodataPropertyMapping copy = new MicrodataPropertyMapping(this);
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
