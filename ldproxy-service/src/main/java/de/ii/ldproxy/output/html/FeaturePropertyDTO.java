/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.output.html;

import com.github.mustachejava.util.DecoratedCollection;

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
