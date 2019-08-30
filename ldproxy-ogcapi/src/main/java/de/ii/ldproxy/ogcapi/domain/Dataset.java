/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableDataset.Builder.class)
public abstract class Dataset {

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    public abstract List<Wfs3Link> getLinks();

    public abstract List<String> getCrs();

    //TODO
    //public abstract List<Wfs3Collection> getCollections();
    @Value.Derived
    public List<Wfs3Collection> getCollections() {
        Optional<List<Wfs3Collection>> collections = getSections().stream()
                                                                 .filter(stringObjectMap -> stringObjectMap.containsKey("collections"))
                                                                 .map(stringObjectMap -> (List<Wfs3Collection>) stringObjectMap.get("collections"))
                                                                 .findFirst();
        if (collections.isPresent()) {
            return collections.get();
        }

        return ImmutableList.of();
    }

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
