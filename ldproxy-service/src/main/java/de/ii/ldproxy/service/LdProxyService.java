/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.ldproxy.output.generic.GenericMapping;
import de.ii.ldproxy.output.generic.Gml2GenericMapper;
import de.ii.ldproxy.output.geojson.GeoJsonFeatureWriter;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMapper;
import de.ii.ldproxy.output.html.Gml2MicrodataMapper;
import de.ii.ldproxy.output.jsonld.Gml2JsonLdMapper;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyService;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureType;
import de.ii.xsf.core.api.JsonViews;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLParser;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class LdProxyService extends AbstractWfsProxyService {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyService.class);
    public static final String SERVICE_TYPE = "ldproxy";
    private static final String INTERFACE_SPECIFICATION = "LinkedDataService";

    //private Map<String, List<String>> indices;
    private LdProxyIndexStore indexStore;
    private SparqlAdapter sparqlAdapter;
    private CodelistStore codelistStore;

    // TODO: we already have external url, can we use it here?
    private Map<String, String> rewrites;

    private String vocab;


    public LdProxyService() {
        this.rewrites = new HashMap<>();
    }

    public LdProxyService(String id, String wfsUrl) {
        super(id, SERVICE_TYPE, null, new WFSAdapter(wfsUrl.trim()));

        this.rewrites = new HashMap<>();
                //this.description = "";
        //String[] path = {orgid};
        //initialize(path, module);

        // TODO: dynamic
        this.schemaAnalyzers.add(new Gml2GenericMapper(this));
        this.schemaAnalyzers.add(new Gml2MicrodataMapper(this));
        this.schemaAnalyzers.add(new Gml2GeoJsonMapper(this));
        this.schemaAnalyzers.add(new Gml2JsonLdMapper(this));

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

    @JsonView(JsonViews.FullView.class)
    public Map<String, String> getRewrites() {
        return rewrites;
    }

    public void setRewrites(Map<String, String> rewrites) {
        this.rewrites = rewrites;
    }

    public Map<String, String> findIndicesForFeatureType(WfsProxyFeatureType ft) {
        return  findIndicesForFeatureType(ft, true);
    }

    public Map<String, String> findIndicesForFeatureType(WfsProxyFeatureType ft, boolean onlyEnabled) {
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

    public Map<String, String> getFilterableFieldsForFeatureType(WfsProxyFeatureType featureType) {
        //return featureType.getMappings().findMappings(TargetMapping.BASE_TYPE).values().stream().filter(mapping -> mapping.get(0).isEnabled()).map(mapping -> mapping.get(0).getName().toLowerCase().replaceAll(" ", "%20")).collect(Collectors.toList());

        return featureType.getMappings().findMappings(TargetMapping.BASE_TYPE).entrySet().stream()
                .filter(mapping -> ((GenericMapping)mapping.getValue().get(0)).isFilterable() && mapping.getValue().get(0).getName() != null && mapping.getValue().get(0).isEnabled())
                .collect(Collectors.toMap(mapping -> mapping.getValue().get(0).getName().toLowerCase(), Map.Entry::getKey));
    }

    public Map<String, String> getHtmlNamesForFeatureType(WfsProxyFeatureType featureType) {

        return featureType.getMappings().findMappings(Gml2MicrodataMapper.MIME_TYPE).entrySet().stream()
                .filter(mapping -> mapping.getValue().get(0).getName() != null && mapping.getValue().get(0).isEnabled())
                .collect(Collectors.toMap(Map.Entry::getKey, mapping -> mapping.getValue().get(0).getName()));
    }

    public String getGeometryPathForFeatureType(WfsProxyFeatureType featureType) {
        //return featureType.getMappings().findMappings(TargetMapping.BASE_TYPE).values().stream().filter(mapping -> mapping.get(0).isEnabled()).map(mapping -> mapping.get(0).getName().toLowerCase().replaceAll(" ", "%20")).collect(Collectors.toList());

        // TODO: provide isGeometry on TargetMapping.BASE_TYPE
        // TODO: does mapping.getValue().get(0).isEnabled() take into account general mapping?
        List<Map.Entry<String,List<TargetMapping>>> geometries = featureType.getMappings().findMappings("application/geo+json").entrySet().stream()
                .filter(mapping -> mapping.getValue().get(0).isGeometry() && mapping.getValue().get(0).isEnabled())
                .collect(Collectors.toList());

        return !geometries.isEmpty() ? geometries.get(0).getKey() : null;
    }

    @JsonIgnore
    public List<String> getIndexValues(WfsProxyFeatureType featureType, String index, String property) {
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
                    LOGGER.getLogger().debug("Error harvesting index", e);
                }
            } else {
                return indexStore.getResource(index).getValues();
            }
        }

        return values;
    }

    private SortedSet<String> harvestIndex(WfsProxyFeatureType featureType, String property) throws ExecutionException {
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
                LOGGER.getLogger().debug("TRYING AGAIN {}", tries);
                continue;
            }

            values.addAll(indexValueWriter.getValues());
            numberMatched = indexValueWriter.getNumberMatched();
            startIndex += count;
            tries = 0;
            LOGGER.getLogger().debug("{}/{}", startIndex, numberMatched);
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
}
