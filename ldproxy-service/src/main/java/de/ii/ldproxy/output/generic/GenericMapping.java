/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.generic;

import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public class GenericMapping implements TargetMapping {

    protected String name;
    protected Boolean enabled;
    protected boolean isGeometry;
    protected String format;
    protected String codelist;
    protected boolean filterable;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isGeometry() {
        return isGeometry;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setGeometry(boolean geometry) {
        isGeometry = geometry;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCodelist() {
        return codelist;
    }

    public void setCodelist(String codelist) {
        this.codelist = codelist;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }
}
