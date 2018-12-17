/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;




/**
 * @author zahnen
 */
@Component
@Provides(specifications = {Wfs3Core.class})
@Instantiate
public class Wfs3Core {
    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Core.class);

    private final List<Wfs3Extension> wfs3Extensions;

    public Wfs3Core(@Requires Wfs3ExtensionRegistry wfs3ExtensionRegistry) {
        this.wfs3Extensions = wfs3ExtensionRegistry.getExtensions();
    }

    public void checkCollectionName(Wfs3ServiceData serviceData, String collectionName) {
        if (!serviceData.isFeatureTypeEnabled(collectionName)) {
            throw new NotFoundException();
        }
    }

    private List<Wfs3CollectionMetadataExtension> getCollectionExtenders() {
        return wfs3Extensions.stream()
                             .filter(wfs3Extension -> wfs3Extension instanceof Wfs3CollectionMetadataExtension)
                             .map(wfs3Extension -> (Wfs3CollectionMetadataExtension) wfs3Extension)
                             .collect(Collectors.toList());
    }

    private List<Wfs3DatasetMetadataExtension> getDatasetExtenders() {
        return wfs3Extensions.stream()
                .filter(wfs3Extension -> wfs3Extension instanceof Wfs3DatasetMetadataExtension)
                .map(wfs3Extension -> (Wfs3DatasetMetadataExtension) wfs3Extension)
                .collect(Collectors.toList());
    }

    public Wfs3Collections createCollections(Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        List<Wfs3Collection> collections = serviceData.getFeatureTypes()
                                                      .values()
                                                      .stream()
                                                      //TODO
                                                      .filter(featureType -> serviceData.isFeatureTypeEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                                                      .map(featureType -> createCollection(featureType, wfs3LinksGenerator, serviceData, mediaType, alternativeMediaTypes, uriCustomizer, true))
                                                      .collect(Collectors.toList());

        ImmutableList<String> crs = ImmutableList.<String>builder()
                .add(serviceData.getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri())
                .add(Wfs3ServiceData.DEFAULT_CRS_URI)
                .addAll(serviceData.getAdditionalCrs()
                                   .stream()
                                   .map(EpsgCrs::getAsUri)
                                   .collect(Collectors.toList()))
                .build();


       List<Wfs3Link> wfs3Links = wfs3LinksGenerator.generateDatasetLinks(uriCustomizer.copy(), Optional.empty()/*new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/, mediaType, alternativeMediaTypes);


        ImmutableWfs3Collections.Builder dataset = ImmutableWfs3Collections.builder()
                .collections(collections)
                .crs(crs)
                .links(wfs3Links);



        for (Wfs3DatasetMetadataExtension wfs3DatasetMetadataExtension: getDatasetExtenders()) {
            dataset = wfs3DatasetMetadataExtension.process(dataset,  uriCustomizer.copy(), serviceData.getFeatureTypes().values());
        }


        return dataset.build();

    }

    public Wfs3Collection createCollection(FeatureTypeConfigurationWfs3 featureType, Wfs3LinksGenerator wfs3LinksGenerator, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, boolean isNested) {


        final String qn = featureType.getLabel()/*service.getWfsAdapter()
                                                               .getNsStore()
                                                               .getNamespacePrefix(featureType.getNamespace()) + ":" + featureType.getName()*/;

        ImmutableWfs3Collection.Builder collection = ImmutableWfs3Collection.builder()
                                                                            .name(featureType.getId())
                                                                            .title(featureType.getLabel())
                                                                            .description(featureType.getDescription())
                                                                            .prefixedName(qn)
                                                                            .links(wfs3LinksGenerator.generateDatasetCollectionLinks(uriCustomizer.copy(), featureType.getId(), featureType.getLabel(), Optional.empty() /* new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType(ImmutableMap.of(featureType.getNamespace(), ImmutableList.of(featureType.getName())))).getAsUrl()*/, mediaType, alternativeMediaTypes));

        if (serviceData.getFilterableFieldsForFeatureType(featureType.getId())
                       .containsKey("time")) {
            collection.extent(new Wfs3Extent(
                    featureType.getExtent()
                               .getTemporal()
                               .getStart(),
                    featureType.getExtent()
                               .getTemporal()
                               .getComputedEnd(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        } else {
            collection.extent(new Wfs3Extent(
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        }

        //TODO: to crs extension
        if (isNested) {
            collection.crs(
                    ImmutableList.<String>builder()
                            .add(serviceData.getFeatureProvider()
                                            .getNativeCrs()
                                            .getAsUri())
                            .add(Wfs3ServiceData.DEFAULT_CRS_URI)
                            .addAll(serviceData.getAdditionalCrs()
                                               .stream()
                                               .map(EpsgCrs::getAsUri)
                                               .collect(Collectors.toList()))
                            .build()
            );
        }

        for (Wfs3CollectionMetadataExtension wfs3CollectionMetadataExtension : getCollectionExtenders()) {
            collection = wfs3CollectionMetadataExtension.process(collection, featureType, uriCustomizer.copy(), isNested,serviceData.getId());
        }

        return collection.build();
    }



}
