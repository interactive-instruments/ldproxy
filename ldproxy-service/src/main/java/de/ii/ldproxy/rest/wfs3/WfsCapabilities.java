/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.wfs3;

import com.github.mustachejava.util.DecoratedCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class WfsCapabilities {
    public String name;
    public String title;
    public String description;
    public List<String> keywords;
    public String version;
    public String license;
    public String bbox;
    public String url;
    public String providerName;
    public String providerUrl;
    public String contactName;
    public String contactEmail;
    public String contactTelephone;
    public String contactFaxNumber;
    public String contactHoursOfService;
    public String contactUrl;
    public String contactStreetAddress;
    public String contactLocality;
    public String contactRegion;
    public String contactPostalCode;
    public String contactCountry;

    public WfsCapabilities() {
        this.keywords = new ArrayList<>();
    }

    public DecoratedCollection<String> getKeywordsDecorated() {
        return new DecoratedCollection<>(keywords);
    }
}
