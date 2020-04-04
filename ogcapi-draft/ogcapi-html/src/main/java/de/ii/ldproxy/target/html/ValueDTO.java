/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import java.util.Objects;

public class ValueDTO implements ObjectOrPropertyOrValueDTO {
    public String value = null;
    public PropertyDTO property = null;

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isNull() {
        return Objects.isNull(value);
    }

    public boolean isHtml() {
        return Objects.isNull(value) ? false : value.startsWith("<") && (value.endsWith(">") || value.endsWith(">\n")) && value.contains("</");
    }

    public boolean isUrl() {
        return Objects.isNull(value) ? false : value.startsWith("http://") || value.startsWith("https://");
    }

    public boolean isImageUrl() {
        return Objects.isNull(value) ? false : isUrl() && (value.toLowerCase()
                .endsWith(".png") || value.toLowerCase()
                .endsWith(".jpg") || value.toLowerCase()
                .endsWith(".jpeg") || value.toLowerCase()
                .endsWith(".gif"));
    }

    public boolean isLevel2() {
        return getLevel()==2;
    }

    public boolean isLevel3() {
        return getLevel()==3;
    }

    private int getLevel() {
        ObjectOrPropertyDTO property = this.property;
        int level = 0;
        while (Objects.nonNull(property)) {
            level++;
            ObjectOrPropertyDTO object = property.parent;
            property = Objects.nonNull(object) ? object.parent : null;
        }
        return level;
    }
}
