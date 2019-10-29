/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class PageRepresentation {

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    public abstract List<OgcApiLink> getLinks();

    @JsonIgnore
    public abstract List<Map<String, Object>> getSections();

    @JsonIgnore
    @Value.Derived
    public List<Map<String, Object>> getOrderedSections() {
        return getSections().stream().sorted(new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> stringObjectMap, Map<String, Object> t1) {
                if (!stringObjectMap.containsKey("sortPriority")) return 1;
                if (!t1.containsKey("sortPriority")) return -1;
                return ((Integer)stringObjectMap.get("sortPriority")) - (((Integer)t1.get("sortPriority")));
            }
        }).collect(Collectors.toList());
    }

    @JsonIgnore
    @Value.Default
    public boolean getSectionsFirst() {
        return false;
    }
}
