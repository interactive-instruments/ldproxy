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
import de.ii.xsf.core.views.GenericView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
public class DatasetView extends GenericView {

    public String name;
    public String title;
    public String description;
    public List<String> keywords;
    public String version;
    public String license;
    public String bbox;
    public String url;
    public List<DatasetView> featureTypes;
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

    public DatasetView(URI uri) {
        this("service", uri);
    }

    public DatasetView(String template, URI uri) {
        this(template, uri, (Object)null);
    }

    public DatasetView(String template, URI uri, Object data) {
        super(template, uri, data);
        this.keywords = new ArrayList<>();
        this.featureTypes = new ArrayList<>();
    }

    public DatasetView(String template, URI uri, String name) {
        this(template, uri, name, name);
    }

    public DatasetView(String template, URI uri, String name, String title) {
        this(template, uri);
        this.name = name;
        this.title = title;
    }

    public DecoratedCollection<String> getKeywordsDecorated() {
        return new DecoratedCollection<>(keywords);
    }
}
