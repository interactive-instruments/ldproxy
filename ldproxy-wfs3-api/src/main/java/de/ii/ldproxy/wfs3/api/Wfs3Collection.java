/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class Wfs3Collection {

    private String name;
    private String title;
    private String description;
    private Wfs3Extent extent;
    private List<Wfs3Link> links;
    private String prefixedName;
    private List<String> crs;
    private Map<String,Object> extensions;

    public Wfs3Collection() {

    }

    public Wfs3Collection(String name, String title, String description, Wfs3Extent extent, List<Wfs3Link> links, String prefixedName) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.extent = extent;
        this.links = links;
        this.prefixedName = prefixedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Wfs3Extent getExtent() {
        return extent;
    }

    public void setExtent(Wfs3Extent extent) {
        this.extent = extent;
    }

    public List<Wfs3Link> getLinks() {
        return links;
    }

    public void setLinks(List<Wfs3Link> links) {
        this.links = links;
    }

    public List<String> getCrs() {
        return crs;
    }

    public void setCrs(List<String> crs) {
        this.crs = crs;
    }

    @JsonIgnore
    public String getPrefixedName() {
        return prefixedName;
    }

    public void setPrefixedName(String prefixedName) {
        this.prefixedName = prefixedName;
    }

    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object extension) {
        if (extensions == null) {
            this.extensions = new LinkedHashMap<>();
        }
        this.extensions.put(name, extension);
    }
}
