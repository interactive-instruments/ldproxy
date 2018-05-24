/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.generic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

/**
 * @author zahnen
 */
public abstract class AbstractGenericMapping<T> implements TargetMapping<T> {

    protected String name;
    protected Boolean enabled;
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
    @JsonIgnore
    public abstract boolean isSpatial();

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
