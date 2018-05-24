/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import de.ii.xtraplatform.feature.query.api.TargetMapping;

/**
 * @author zahnen
 */
public class IndexMapping implements TargetMapping<Object> {
    public static final String MIME_TYPE = "index";


    private Boolean enabled;
    private String name;

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

    @Override
    public Object getType() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        return this;
    }

    //TODO
    @Override
    public boolean isSpatial() {
        return false;
    }
}
