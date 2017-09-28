/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
