/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

/**
 * @author zahnen
 */
public class MicrodataPropertyMapping implements MicrodataMapping {

    private Boolean enabled;
    private String name;
    private MICRODATA_TYPE type;
    private Boolean showInCollection;
    private String itemType;
    private String itemProp;
    private String sparqlQuery;
    private String format;

    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public boolean isGeometry() {
        return getType() == MICRODATA_TYPE.GEOMETRY;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
