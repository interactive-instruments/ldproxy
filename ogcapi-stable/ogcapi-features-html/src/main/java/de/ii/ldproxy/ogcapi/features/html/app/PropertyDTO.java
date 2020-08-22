/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PropertyDTO extends ObjectOrPropertyDTO {

    public List<ValueDTO> values = new ArrayList<>();
    public String baseName = null;

    public void addValue(ValueDTO value) {
        values.add(value);
        value.property = this;
    }

    public ValueDTO addValue(String value) {
        ValueDTO newValue = new ValueDTO();
        newValue.setValue(value);
        this.addValue(newValue);
        return newValue;
    }

    public boolean hasValues() {
        return !values.isEmpty() && values.stream().anyMatch(value -> !value.isNull());
    }

    public String getFirstValue() {
        return hasValues() ? values.get(0).value : null;
    }

    public ImmutableList<ObjectDTO> objectValues() {
        return childList.stream()
                .filter(child -> child instanceof ObjectDTO && ((ObjectDTO)child).hasNonNullProperty())
                .map(child -> (ObjectDTO)child)
                .collect(ImmutableList.toImmutableList());
    }

    public boolean isLevel1() {
        return getLevel()==1;
    }

    public boolean isLevel2() {
        return getLevel()==2;
    }

    public boolean isLevel3() {
        return getLevel()==3;
    }

    public boolean isLevel4() {
        return getLevel()==4;
    }

    public boolean isLevel5() {
        return getLevel()==5;
    }

    public int getLevel() {
        ObjectOrPropertyDTO property = this;
        int level = 0;
        while (Objects.nonNull(property)) {
            level++;
            ObjectOrPropertyDTO object = property.parent;
            property = Objects.nonNull(object) ? object.parent : null;
        }
        return level;
    }
}
