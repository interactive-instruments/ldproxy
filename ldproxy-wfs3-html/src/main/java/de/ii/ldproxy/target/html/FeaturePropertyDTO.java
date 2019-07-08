/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class FeaturePropertyDTO {
    public String itemType;
    public String itemProp;
    public String name;
    public String value;
    public boolean isUrl;
    public boolean isImg;
    public boolean isHtml;
    public List<FeaturePropertyDTO> childList;
    public FeaturePropertyDTO parent;

    public FeaturePropertyDTO() {
        this.childList = new ArrayList<>();
    }

    public String microdata() {
        String microdata = "";

        if (itemProp != null && !itemProp.isEmpty()) {
            microdata += "itemprop=\"" + itemProp + "\"";
        }
        if (itemType != null && !itemType.isEmpty()) {
            microdata += " itemscope itemtype=\"" + itemType + "\"";
        }

        return microdata;
    }

    public SplitDecoratedCollection<FeaturePropertyDTO> children() {
        return childList.size() > 0 ? new SplitDecoratedCollection<FeaturePropertyDTO>(childList) : null;
    }

    public void addChild(FeaturePropertyDTO child) {
        childList.add(child);
        child.parent = this;
    }
}
