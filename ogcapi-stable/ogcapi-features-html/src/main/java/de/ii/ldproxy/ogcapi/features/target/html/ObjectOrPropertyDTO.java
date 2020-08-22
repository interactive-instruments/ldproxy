/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.html;

import de.ii.ldproxy.ogcapi.html.domain.SplitDecoratedCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ObjectOrPropertyDTO implements Comparable<ObjectOrPropertyDTO>,  ObjectOrPropertyOrValueDTO {
    public String itemType = null;
    public String itemProp = null;
    public String name = null;
    public List<ObjectOrPropertyDTO> childList = new ArrayList<>();
    public ObjectOrPropertyDTO parent = null;
    public int sortPriority = Integer.MAX_VALUE;

    public SplitDecoratedCollection<ObjectOrPropertyDTO> children() {
        return childList.size() > 0 ? new SplitDecoratedCollection<ObjectOrPropertyDTO>(childList) : null;
    }

    public String getSchemaOrgItemType() {
        return Objects.nonNull(itemType) && itemType.startsWith("http://schema.org/") ? itemType.substring(18) : null;
    }

    public void addChild(ObjectOrPropertyDTO child) {
        childList.add(child);
        child.parent = this;
    }

    @Override
    public int compareTo(ObjectOrPropertyDTO other) {
        if (this.parent!=other.parent || this==other)
            return 0;
        return this.sortPriority - other.sortPriority;
    }
}
