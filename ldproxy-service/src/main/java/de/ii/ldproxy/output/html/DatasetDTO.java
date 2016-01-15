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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author zahnen
 */
public class DatasetDTO {
    public String name;
    public String title;
    public String description;
    public List<String> keywords;
    public String version;
    public String license;
    public String bbox;
    public String url;
    public List<DatasetDTO> featureTypes;
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
    public List<NavigationDTO> breadCrumbs;
    public List<NavigationDTO> formats;

    public DatasetDTO() {
        this.keywords = new ArrayList<>();
        this.featureTypes = new ArrayList<>();
    }

    public DatasetDTO(String name) {
        this();
        this.name = name;
        this.title = name;
    }
}
