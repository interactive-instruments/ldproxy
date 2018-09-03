/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author zahnen
 */

public class Wfs3ConformanceClasses {
    private final List<String> classes;

    public Wfs3ConformanceClasses() {
        this.classes = ImmutableList.of();
    }

    public Wfs3ConformanceClasses(List<String> classes) {this.classes = classes;}

    public List<String> getConformsTo() {
        return classes;
    }
}
