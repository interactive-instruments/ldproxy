/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.wfs3;

import com.google.common.collect.ImmutableList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collection;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collections")
public class Wfs3Collections {

    private List<Wfs3Collection> collections;

    public Wfs3Collections() {}

    public Wfs3Collections(Collection<Wfs3Collection> collections) {
        this.collections = ImmutableList.copyOf(collections);
    }

    @XmlElement(name = "Collection")
    public List<Wfs3Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Wfs3Collection> collections) {
        this.collections = collections;
    }

    @XmlType(propOrder={"name","title","description", "extent"})
    public static class Wfs3Collection {

        static class Extent {
            private double[] bbox;

            public Extent(String xmin, String ymin, String xmax, String ymax) {
                try {
                    this.bbox = ImmutableList.of(xmin, ymin, xmax, ymax).stream().mapToDouble(Double::parseDouble).toArray();
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            public double[] getBbox() {
                return bbox;
            }

            public void setBbox(double[] bbox) {
                this.bbox = bbox;
            }
        }

        private String name;
        private String title;
        private String description;
        private Extent extent;

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

        public Extent getExtent() {
            return extent;
        }

        public void setExtent(Extent extent) {
            this.extent = extent;
        }
    }
}
