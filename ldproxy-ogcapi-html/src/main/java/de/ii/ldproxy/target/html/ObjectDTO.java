/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import java.util.Objects;

public class ObjectDTO extends ObjectOrPropertyDTO {
    public PropertyDTO id = null;
    public PropertyDTO geo = null;
    public boolean inCollection = false;

    // relevant for related objects only ("around relations")
    public PropertyDTO links = null;
    public String additionalParams = null;

    public String getGeoAsString() {
        if (Objects.nonNull(geo)) {
            if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoShape")) {
                PropertyDTO geoprop = (PropertyDTO) geo.childList.get(0);
                String geomType = geoprop.itemProp;
                String coords = geoprop.values.get(0).value;
                if (Objects.nonNull(geomType) && Objects.nonNull(coords))
                    return "{ \"@type\": \"GeoShape\", \"" +
                            geomType + "\": \"" + coords + "\" }";
            } else if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoCoordinates")) {
                String latitude = ((PropertyDTO) geo.childList.get(0)).values.get(0).value;
                String longitude = ((PropertyDTO) geo.childList.get(1)).values.get(0).value;
                if (Objects.nonNull(latitude) && Objects.nonNull(longitude))
                    return "{ \"@type\": \"GeoCoordinates\", \"latitude\": \"" + latitude + "\", \"longitude\": \"" + longitude + "\" }";
            }
        }

        return null;
    }

    public PropertyDTO get(int sortPriority) {
        return (PropertyDTO) childList.stream()
                .filter(prop -> prop instanceof PropertyDTO && prop.sortPriority==sortPriority)
                .findFirst()
                .orElse(null);
    }
}
