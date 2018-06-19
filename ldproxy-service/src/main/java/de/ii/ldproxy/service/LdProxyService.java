/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.ldproxy.output.generic.GenericMapping;
import de.ii.ldproxy.output.generic.Gml2GenericMappingProvider;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMappingProvider;
import de.ii.ldproxy.output.html.Gml2MicrodataMappingProvider;
import de.ii.ldproxy.output.jsonld.Gml2JsonLdMappingProvider;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyService;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzerFromData;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzerFromSchema;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLParser;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class LdProxyService extends AbstractWfsProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdProxyService.class);
    public static final String SERVICE_TYPE = "ldproxy";
    private static final String INTERFACE_SPECIFICATION = "LinkedDataService";

    //private Map<String, List<String>> indices;
    private LdProxyIndexStore indexStore;
    private SparqlAdapter sparqlAdapter;
    private CodelistStore codelistStore;

    private String vocab;
    private Boolean secured;


    public LdProxyService() {

    }

    public LdProxyService(String id, String wfsUrl) {
        super(id, SERVICE_TYPE, null, new WFSAdapter(wfsUrl.trim()));

                //this.description = "";
        //String[] path = {orgid};
        //initialize(path, module);

        // TODO: dynamic
        List<TargetMappingProviderFromGml> mappingProviders = ImmutableList.of(new Gml2GenericMappingProvider(), new Gml2MicrodataMappingProvider(), new Gml2GeoJsonMappingProvider(), new Gml2JsonLdMappingProvider());
        this.schemaAnalyzers.add(new WfsProxyFeatureTypeAnalyzerFromSchema(this, mappingProviders));
        this.mappingFromDataAnalyzers = new WfsProxyFeatureTypeAnalyzerFromData(this, mappingProviders);

        // TODO
        //this.analyzeWFS();
    }

    public final void initialize(LdProxyIndexStore indexStore, SparqlAdapter sparqlAdapter, CodelistStore codelistStore) {
        this.indexStore = indexStore;
        this.sparqlAdapter = sparqlAdapter;
        this.codelistStore = codelistStore;
    }

    @Override
    public String getInterfaceSpecification() {
        return INTERFACE_SPECIFICATION;
    }

    public Map<String, String> findIndicesForFeatureType(FeatureTypeConfiguration ft) {
        return  findIndicesForFeatureType(ft, true);
    }

    public Map<String, String> findIndicesForFeatureType(FeatureTypeConfiguration ft, boolean onlyEnabled) {
        Map<String, String> indices = new HashMap<>();

        Map<String, List<TargetMapping>> mappings = ft.getMappings().findMappings(IndexMapping.MIME_TYPE);
        for(String path: mappings.keySet()) {
            for (TargetMapping mapping: mappings.get(path)) {
                if (!onlyEnabled || mapping.isEnabled()) {
                    indices.put(mapping.getName(), path);
                }
            }
        }

        return indices;
    }

    public Map<String, String> getFilterableFieldsForFeatureType(FeatureTypeConfiguration featureType) {
        return getFilterableFieldsForFeatureType(featureType, false);
    }

    public Map<String, String> getFilterableFieldsForFeatureType(FeatureTypeConfiguration featureType, boolean withoutSpatialAndTemporal) {
        return featureType.getMappings().findMappings(GenericMapping.BASE_TYPE).entrySet().stream()
                .filter(isFilterable(withoutSpatialAndTemporal))
                .collect(Collectors.toMap(getParameterName(), mapping -> mapping.getKey().substring(mapping.getKey().lastIndexOf(":")+1)));
    }

    private Function<Map.Entry<String, List<TargetMapping>>, String> getParameterName() {
        return mapping -> ((GenericMapping)mapping.getValue().get(0)).isSpatial() ? "bbox"
                : ((GenericMapping)mapping.getValue().get(0)).isTemporal() ? "time"
                : mapping.getValue().get(0).getName().toLowerCase();
    }

    private Predicate<Map.Entry<String, List<TargetMapping>>> isFilterable(boolean withoutSpatialAndTemporal) {
        return mapping -> ((GenericMapping)mapping.getValue().get(0)).isFilterable() &&
                (mapping.getValue().get(0).getName() != null || ((GenericMapping)mapping.getValue().get(0)).isSpatial()) &&
                mapping.getValue().get(0).isEnabled() &&
                (!withoutSpatialAndTemporal || (!((GenericMapping)mapping.getValue().get(0)).isSpatial() && !((GenericMapping)mapping.getValue().get(0)).isTemporal()));
    }

    public Map<String, String> getHtmlNamesForFeatureType(FeatureTypeConfiguration featureType) {

        return featureType.getMappings().findMappings(Gml2MicrodataMappingProvider.MIME_TYPE).entrySet().stream()
                .filter(mapping -> mapping.getValue().get(0).getName() != null && mapping.getValue().get(0).isEnabled())
                .collect(Collectors.toMap(Map.Entry::getKey, mapping -> mapping.getValue().get(0).getName()));
    }

    public Optional<String> getSpatialFieldPathForFeatureType(FeatureTypeConfiguration featureType) {
        //return featureType.getMappings().findMappings(TargetMapping.BASE_TYPE).values().stream()
        // .filter(mapping -> mapping.get(0).isEnabled()).map(mapping -> mapping.get(0).getName().toLowerCase().replaceAll(" ", "%20")).collect(Collectors.toList());

        // TODO: provide isGeometry on TargetMapping.BASE_TYPE
        // TODO: does mapping.getValue().get(0).isEnabled() take into account general mapping?
        return featureType.getMappings().findMappings(GenericMapping.BASE_TYPE).entrySet().stream()
                .filter(mapping -> ((GenericMapping)mapping.getValue().get(0)).isSpatial() && mapping.getValue().get(0).isEnabled())
                .map(Map.Entry::getKey)
                .findFirst();

        //return !geometries.isEmpty() ? geometries.get(0).getKey() : null;
    }

    public Optional<String> getTemporalFieldPathForFeatureType(FeatureTypeConfiguration featureType, boolean onlyFilterable) {
        return featureType.getMappings().findMappings(GenericMapping.BASE_TYPE).entrySet().stream()
                          .filter(mapping -> ((GenericMapping)mapping.getValue().get(0)).isTemporal() && mapping.getValue().get(0).isEnabled() && (!onlyFilterable || ((GenericMapping) mapping.getValue().get(0)).isFilterable()))
                          .map(Map.Entry::getKey)
                          .findFirst();
    }

    @JsonIgnore
    public List<String> getIndexValues(FeatureTypeConfiguration featureType, String index, String property) {
        List<String> values = new ArrayList<>();

        if(findIndicesForFeatureType(featureType).containsKey(index)) {
            if (!indexStore.hasResource(index)) {
                try {
                    values.addAll(harvestIndex(featureType, property));

                    PropertyIndex propertyIndex = new PropertyIndex();
                    propertyIndex.setResourceId(index);
                    propertyIndex.setValues(values);

                    indexStore.addResource(propertyIndex);
                } catch (ExecutionException | IOException e) {
                    LOGGER.debug("Error harvesting index", e);
                }
            } else {
                return indexStore.getResource(index).getValues();
            }
        }

        return values;
    }

    private SortedSet<String> harvestIndex(FeatureTypeConfiguration featureType, String property) throws ExecutionException {
        // TODO: only if WFS 2.0.0, else GetFeature
        // TODO: seems to be incomplete, try GetFeature with paging

        String propertyName = property.substring(property.lastIndexOf(':')+1);

        SortedSet<String> values = new TreeSet<>();
        int count = 15000;
        int startIndex = 0;
        int numberMatched = 1;
        int tries = 0;

        //WFSOperation operation = new GetPropertyValuePaging(getWfsAdapter().getNsStore().getNamespacePrefix(featureType.getNamespace()), featureType.getName(), propertyName, -1, 0);
        while (numberMatched > 0 && startIndex < numberMatched) {
            WFSOperation operation = new GetFeaturePaging(getWfsAdapter().getNsStore().getNamespacePrefix(featureType.getNamespace()), featureType.getName(), count, startIndex);
            WFSRequest request = new WFSRequest(getWfsAdapter(), operation);

            IndexValueWriter indexValueWriter = new IndexValueWriter(propertyName);

            GMLParser gmlParser = new GMLParser(indexValueWriter, staxFactory);
            try {
                gmlParser.parse(request.getResponse(), WFS.getNS(WFS.VERSION._2_0_0), "member");
            } catch(Throwable e) {
                // ignore
            }
            if(indexValueWriter.hasFailed() && tries < 3) {
                tries++;
                LOGGER.debug("TRYING AGAIN {}", tries);
                continue;
            }

            values.addAll(indexValueWriter.getValues());
            numberMatched = indexValueWriter.getNumberMatched();
            startIndex += count;
            tries = 0;
            LOGGER.debug("{}/{}", startIndex, numberMatched);
        }

        return values;
    }

    //@JsonView(JsonViews.FullView.class)
    @JsonIgnore
    public SparqlAdapter getSparqlAdapter() {
        return sparqlAdapter;
    }

    @JsonIgnore
    public CodelistStore getCodelistStore() {
        return codelistStore;
    }

    public String getVocab() {
        return vocab;
    }

    public void setVocab(String vocab) {
        this.vocab = vocab;
    }

    @JsonIgnore
    public boolean isSecured() {
        return secured != null && secured;
    }

    @JsonProperty
    public Boolean getSecured() {
        return secured;
    }

    public void setSecured(Boolean secured) {
        this.secured = secured;
    }
}
