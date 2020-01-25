/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import java.util.ArrayList;
import java.util.List;

public class PropertyDTO extends ObjectOrPropertyDTO {

    public List<ValueDTO> values = new ArrayList<>();

    public void addValue(ValueDTO value) {
        values.add(value);
        value.property = this;
    }

    public ValueDTO addValue(String value) {
        return addValue(value, false);
    }

    public ValueDTO addValue(String value, boolean isHtml) {
        ValueDTO newValue = new ValueDTO();
        newValue.setValue(value, isHtml);
        this.addValue(newValue);
        return newValue;
    }

}
