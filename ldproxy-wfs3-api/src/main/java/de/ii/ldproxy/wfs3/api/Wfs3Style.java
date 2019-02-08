/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

public class Wfs3Style {

    private final String styleId;
    private final String styleUrl;

    public Wfs3Style(String styleId, String styleUrl){
        this.styleId = styleId;
        this.styleUrl = styleUrl;
    }

    public String getStyleId(){
        return styleId;
    }

    public String getStyleUrl(){
        return styleUrl;
    }
}
