/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import java.util.Objects;

/**
 * @author zahnen
 */
public class FeatureDTO extends FeaturePropertyDTO {
    public FeaturePropertyDTO id;
    //public final List<FeaturePropertyDTO> properties;
    public FeaturePropertyDTO geo;
    public FeaturePropertyDTO links;
    public boolean titleAsLink;
    public boolean noUrlClosingSlash;
    public String additionalParams;

    /*public FeatureDTO() {
        this.properties = new ArrayList<>();
    }*/

    public String getGeoAsString() {
        if (Objects.nonNull(geo)) {
            if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoShape")) {
                String geomType = geo.childList.get(0).itemProp;
                String coords = geo.childList.get(0).value;
                if (Objects.nonNull(geomType) && Objects.nonNull(coords))
                    return "{ \"@type\": \"GeoShape\", \"" +
                            geomType + "\": \"" + coords + "\" }";
            } else if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoCoordinates")) {
                String latitude = geo.childList.get(0).value;
                String longitude = geo.childList.get(1).value;
                if (Objects.nonNull(latitude) && Objects.nonNull(longitude))
                    return "{ \"@type\": \"GeoCoordinates\", \"latitude\": \"" + latitude + "\", \"longitude\": \"" + longitude + "\" }";
            }
        }

        return null;
    }
}
