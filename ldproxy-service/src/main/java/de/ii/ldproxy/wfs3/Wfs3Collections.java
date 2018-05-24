/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.ogc.api.wfs.client.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collections")
@XmlType(propOrder = {"links", "collections"})
public class Wfs3Collections extends Wfs3Xml {
    private List<Wfs3Link> links;
    private List<Wfs3Collection> collections;
    private WfsCapabilities wfsCapabilities;

    public Wfs3Collections() {
        super();
    }

    public Wfs3Collections(final URICustomizer uriCustomizer, final LdProxyService service, final String mediaType, final String... alternativeMediaTypes) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        this.collections = service.getFeatureTypes()
                                  .values()
                                  .stream()
                                  .filter(isFeatureTypeEnabled(service))
                                  .sorted(Comparator.comparing(FeatureTypeConfiguration::getName))
                                  .map(featureType -> {
                                      final Wfs3Collection collection = new Wfs3Collection();

                                      final String qn = service.getWfsAdapter()
                                                               .getNsStore()
                                                               .getNamespacePrefix(featureType.getNamespace()) + ":" + featureType.getName();

                                      collection.setName(featureType.getName()
                                                                    .toLowerCase());
                                      collection.setTitle(featureType.getDisplayName());
                                      collection.setPrefixedName(qn);
                                      collection.setLinks(wfs3LinksGenerator.generateDatasetCollectionLinks(uriCustomizer.copy(), mediaType, featureType.getName()
                                                                                                                                                        .toLowerCase(), featureType.getDisplayName(), new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType(ImmutableMap.of(featureType.getNamespace(), ImmutableList.of(featureType.getName())))).getAsUrl()));
                                      collection.setExtent(new Wfs3Collection.Extent(featureType.getTemporalExtent().getStart(), featureType.getTemporalExtent().getComputedEnd()));
                                      return collection;
                                  })
                                  .collect(Collectors.toList());;

        this.wfsCapabilities = new WfsCapabilities();
        WFSOperation operation = new GetCapabilities();
        WFSCapabilitiesAnalyzer analyzer = new GetCapabilities2Wfs3Dataset(this);
        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);
        wfsParser.parse(service.getWfsAdapter()
                               .request(operation));

        // TODO: apply local information (title, enabled, etc.)

        this.links =  wfs3LinksGenerator.generateDatasetLinks(uriCustomizer.copy(), new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl(), mediaType, alternativeMediaTypes);
    }

    private Predicate<FeatureTypeConfiguration> isFeatureTypeEnabled(final LdProxyService service) {
        return featureType -> {
            final List<TargetMapping> mappings = featureType.getMappings()
                                                            .findMappings(featureType.getNamespace() + ":" + featureType.getName(), TargetMapping.BASE_TYPE);
            return !service.getServiceProperties()
                           .getMappingStatus()
                           .isEnabled() || (!mappings.isEmpty() && mappings.get(0)
                                                                              .isEnabled());
        };
    }

    public Wfs3Collections(Collection<Wfs3Collection> collections, List<Wfs3Link> links) {
        this.collections = ImmutableList.copyOf(collections);
        this.links = ImmutableList.copyOf(links);
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getLinks() {
        return links;
    }

    public void setLinks(List<Wfs3Link> links) {
        this.links = links;
    }

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

}
