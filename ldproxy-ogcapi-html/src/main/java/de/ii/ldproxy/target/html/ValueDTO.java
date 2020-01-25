/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

public class ValueDTO implements ObjectOrPropertyOrValueDTO {
    public String value = null;
    public boolean isUrl = false ;
    public boolean isImg = false;
    public boolean isHtml = false;
    public PropertyDTO property = null;

    public void setValue(String value) {
        setValue(value, false);
    }

    public void setValue(String value, boolean isHtml) {
        this.value = value;
        this.isHtml = isHtml;

        if (value.startsWith("http://") || value.startsWith("https://")) {
            if (value.toLowerCase()
                    .endsWith(".png") || value.toLowerCase()
                    .endsWith(".jpg") || value.toLowerCase()
                    .endsWith(".jpeg") || value.toLowerCase()
                    .endsWith(".gif")) {
                this.isImg = true;
            } else {
                this.isUrl = true;
            }
        }
    }

}
