/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.domain.AbstractOgcApiFeaturesGenericMapping;
import de.ii.ldproxy.ogcapi.domain.OgcApiFeaturesGenericMapping;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;

/**
 * @author zahnen
 */
public class MicrodataPropertyMapping extends AbstractOgcApiFeaturesGenericMapping<MicrodataMapping.MICRODATA_TYPE> implements MicrodataMapping {

    private MICRODATA_TYPE type;
    private Boolean showInCollection;
    private String itemType;
    private String itemProp;

    public MicrodataPropertyMapping() {
    }

    public MicrodataPropertyMapping(MicrodataPropertyMapping mapping) {
        this.enabled = mapping.enabled;
        this.name = mapping.name;
        this.type = mapping.type;
        this.showInCollection = mapping.showInCollection;
        this.itemType = mapping.itemType;
        this.itemProp = mapping.itemProp;
        this.format = mapping.format;
        this.codelist = mapping.codelist;

        //TODO
        this.baseMapping = mapping.baseMapping;
    }

    @Override
    public MICRODATA_TYPE getType() {
        return type;
    }

    public void setType(MICRODATA_TYPE type) {
        this.type = type;
    }

    @Override
    public Boolean isShowInCollection() {
        return showInCollection;
    }

    public void setShowInCollection(boolean showInCollection) {
        this.showInCollection = showInCollection;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public String getItemProp() {
        return itemProp;
    }

    public void setItemProp(String itemProp) {
        this.itemProp = itemProp;
    }

    @Override
    public boolean isSpatial() {
        return getType() == MICRODATA_TYPE.GEOMETRY;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        super.mergeCopyWithBase(targetMapping);

        MicrodataPropertyMapping copy = new MicrodataPropertyMapping(this);
        OgcApiFeaturesGenericMapping baseMapping = (OgcApiFeaturesGenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }
        if ((copy.name == null || copy.name.isEmpty()) && baseMapping.getName() != null) {
            copy.name = baseMapping.getName().replaceAll("\\[\\]", "");
        }
        if ((copy.format == null || copy.format.isEmpty()) && baseMapping.getFormat() != null) {
            copy.format = baseMapping.getFormat();
        }
        copy.codelist = baseMapping.getCodelist();

        return copy;
    }
}
