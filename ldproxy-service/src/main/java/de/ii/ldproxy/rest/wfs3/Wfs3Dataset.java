/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.wfs3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Dataset")
public class Wfs3Dataset {
    private List<Wfs3Link> links;
    private List<Wfs3Collection> collections;
    private WfsCapabilities wfsCapabilities;

    public Wfs3Dataset(final URI requestUri, final LdProxyService service, final String mediaType, final String... alternativeMediaTypes) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        this.collections = service.getFeatureTypes()
               .values()
               .stream()
               .filter(isFeatureTypeEnabled(service))
               .sorted(Comparator.comparing(WfsProxyFeatureType::getName))
               .map(featureType -> {
                   final Wfs3Collection collection = new Wfs3Dataset.Wfs3Collection();

                   final String qn = service.getWfsAdapter()
                                      .getNsStore()
                                      .getNamespacePrefix(featureType.getNamespace()) + ":" + featureType.getName();

                   collection.setName(featureType.getName()
                                        .toLowerCase());
                   collection.setTitle(featureType.getDisplayName());
                   collection.setPrefixedName(qn);
                   collection.setLinks(wfs3LinksGenerator.generateDatasetCollectionLinks(requestUri, mediaType, featureType.getName()
                                                                                                                  .toLowerCase(), featureType.getDisplayName(), new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType(ImmutableMap.of(featureType.getNamespace(), ImmutableList.of(featureType.getName())))).getAsUrl()));
                   return collection;
               })
        .collect(Collectors.toList());

        this.wfsCapabilities = new WfsCapabilities();
        WFSOperation operation = new GetCapabilities();
        WFSCapabilitiesAnalyzer analyzer = new GetCapabilities2Wfs3Dataset(this);
        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);
        wfsParser.parse(service.getWfsAdapter()
                               .request(operation));

        // TODO: apply local information (title, enabled, etc.)

        this.links =  wfs3LinksGenerator.generateDatasetLinks(requestUri, new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl(), mediaType, alternativeMediaTypes);
    }

    private Predicate<WfsProxyFeatureType> isFeatureTypeEnabled(final LdProxyService service) {
        return featureType -> {
            final List<TargetMapping> mappings = featureType.getMappings()
                                                            .findMappings(featureType.getNamespace() + ":" + featureType.getName(), TargetMapping.BASE_TYPE);
            return !service.getServiceProperties()
                           .getMappingStatus()
                           .isEnabled() || (!mappings.isEmpty() && mappings.get(0)
                                                                              .isEnabled());
        };
    }

    public Wfs3Dataset(Collection<Wfs3Collection> collections, List<Wfs3Link> links) {
        this.collections = ImmutableList.copyOf(collections);
        this.links = ImmutableList.copyOf(links);
    }

    @XmlElementWrapper(name = "Links")
    @XmlElement(name = "Link")
    public List<Wfs3Link> getLinks() {
        return links;
    }

    public void setLinks(List<Wfs3Link> links) {
        this.links = links;
    }

    @XmlElementWrapper(name = "Collections")
    @XmlElement(name = "Collection")
    public List<Wfs3Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Wfs3Collection> collections) {
        this.collections = collections;
    }

    @JsonIgnore
    @XmlTransient
    public WfsCapabilities getWfsCapabilities() {
        return wfsCapabilities;
    }

    public void setWfsCapabilities(WfsCapabilities wfsCapabilities) {
        this.wfsCapabilities = wfsCapabilities;
    }

    @XmlType(propOrder = {"name", "title", "description", "extent", "links"})
    public static class Wfs3Collection {

        protected static class Extent {
            private double[] bbox;

            public Extent(String xmin, String ymin, String xmax, String ymax) {
                try {
                    this.bbox = ImmutableList.of(xmin, ymin, xmax, ymax)
                                             .stream()
                                             .mapToDouble(Double::parseDouble)
                                             .toArray();
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
        private List<Wfs3Link> links;
        private String prefixedName;

        public Wfs3Collection() {

        }

        public Wfs3Collection(String name, String title, String description, Extent extent, List<Wfs3Link> links, String prefixedName) {
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

        public Extent getExtent() {
            return extent;
        }

        public void setExtent(Extent extent) {
            this.extent = extent;
        }

        public List<Wfs3Link> getLinks() {
            return links;
        }

        public void setLinks(List<Wfs3Link> links) {
            this.links = links;
        }

        @JsonIgnore
        @XmlTransient
        public String getPrefixedName() {
            return prefixedName;
        }

        public void setPrefixedName(String prefixedName) {
            this.prefixedName = prefixedName;
        }
    }
}
