/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;


public abstract class AbstractOgcApiFeaturesGenericMapping<T extends Enum<T>> implements TargetMapping<T> {

    protected String name;
    protected Boolean enabled;
    protected String format;
    protected String codelist;
    protected boolean filterable;
    private Integer sortPriority;
    protected TargetMapping baseMapping;

    @Override
    public Integer getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(Integer sortPriority) {
        this.sortPriority = sortPriority;
    }


    @Override
    public String getName() {
        return name; // TODO support i18n
    }

    @Override
    @JsonProperty(value = "enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    @JsonIgnore
    public abstract boolean isSpatial();

    public void setName(String name) {
        this.name = name;
    } // TODO support i18n

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setPattern(String format) {
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

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        TargetMapping.super.mergeCopyWithBase(targetMapping);
        return this;
    }

    @Override
    public TargetMapping getBaseMapping() {
        return baseMapping;
    }

    @Override
    public void setBaseMapping(TargetMapping targetMapping) {
        this.baseMapping = targetMapping;
    }
}
