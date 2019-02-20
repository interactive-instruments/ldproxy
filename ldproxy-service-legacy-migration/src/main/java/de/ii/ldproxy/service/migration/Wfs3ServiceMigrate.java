/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping;
import de.ii.ldproxy.target.geojson.GeoJsonMapping;
import de.ii.ldproxy.target.geojson.GeoJsonPropertyMapping;
import de.ii.ldproxy.target.geojson.Gml2GeoJsonMappingProvider;
import de.ii.ldproxy.target.html.Gml2MicrodataMappingProvider;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping;
import de.ii.ldproxy.target.html.MicrodataMapping;
import de.ii.ldproxy.target.html.MicrodataPropertyMapping;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeExtent;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.jsonld.Gml2JsonLdMappingProvider;
import de.ii.xsf.configstore.api.KeyNotFoundException;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableFeatureProviderDataWfs;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TemporalExtent;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zahnen
 */
@Component
@Instantiate
public class Wfs3ServiceMigrate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceMigrate.class);

    private static final String[] PATH = {"ldproxy-services"};

    @Requires
    private EntityRepository entityRepository;

    @Requires
    private KeyValueStore rootConfigStore;

    @Requires
    private Jackson jackson;

    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Validate
    private void onStart() {
        KeyValueStore serviceStore = rootConfigStore.getChildStore(PATH);

        EntityRepositoryForType serviceRepository = new EntityRepositoryForType(entityRepository, Wfs3Service.ENTITY_TYPE);

        executorService.schedule(() -> {

            serviceStore.getKeys()
                        .forEach(id -> {
                            LOGGER.debug("MIGRATING service {} to new schema ...", id);

                            if (serviceRepository.hasEntity(id)) {
                                LOGGER.debug("ALREADY EXISTS, SKIPPING");
                                return;
                            }

                            try {
                                Map<String, Object> service = jackson.getDefaultObjectMapper()
                                                                     .readValue(serviceStore.getValueReader(id), new TypeReference<LinkedHashMap>() {
                                                                     });

                                Wfs3ServiceData wfs3ServiceData = createServiceData(service, null);


                                try {
                                    Map<String, Object> serviceOverrides = jackson.getDefaultObjectMapper()
                                                                                  .readValue(serviceStore.getChildStore("#overrides#")
                                                                                                         .getValueReader(id), new TypeReference<LinkedHashMap>() {
                                                                                  });

                                    wfs3ServiceData = createServiceData(serviceOverrides, wfs3ServiceData);

                                } catch (KeyNotFoundException e) {
                                    //ignore
                                }


                                try {
                                    serviceRepository.createEntity(wfs3ServiceData);
                                    LOGGER.debug("MIGRATED");
                                } catch (IOException e) {
                                    LOGGER.error("ERROR", e);
                                }

                            } catch (Throwable e) {
                                LOGGER.error("ERROR", e);
                            }


                        });

            /*ldProxyServiceStore.getResourceIds()
                               .forEach(id -> {
                                   LOGGER.debug("MIGRATING service {} to new schema ...", id);

                                   EntityRepositoryForType repository = new EntityRepositoryForType(entityRepository, Wfs3Service.ENTITY_TYPE);

                                   if (repository.hasEntity(id)) {
                                       LOGGER.debug("ALREADY EXISTS");
                                       return;
                                   }

                                   LdProxyService service = ldProxyServiceStore.getResource(id);


                               });*/
        }, 10, TimeUnit.SECONDS);
    }

    private Wfs3ServiceData createServiceData(Map<String, Object> service, Wfs3ServiceData wfs3ServiceData) throws URISyntaxException {
        Map<String, Object> wfs = (Map<String, Object>) service.get("wfsAdapter");
        Map<String, Object> defaultCrs = (Map<String, Object>) wfs.get("defaultCrs");
        String url = (String) ((Map<String, Object>) ((Map<String, Object>) wfs.get("urls"))
                .get("GetFeature")).get("GET");
        URI uri = new URI(url);

        ImmutableWfs3ServiceData.Builder builder = ImmutableWfs3ServiceData.builder();

        if (wfs3ServiceData != null) {
            builder.from(wfs3ServiceData);
        } else {
            builder
                    .id((String) service.get("id"))
                    .createdAt((Long) service.get("dateCreated"))
                    .shouldStart(true)
                    .serviceType("WFS3");
        }

        return builder
                .label((String) service.get("name"))
                .description((String) service.get("description"))
                .lastModified((Long) service.get("lastModified"))
                .featureTypes(
                        ((Map<String, Object>) service.get("featureTypes"))
                                .entrySet()
                                .stream()
                                .map(entry -> {
                                    Map<String, Object> ft = (Map<String, Object>) entry.getValue();
                                    Map<String, Object> temporal = (Map<String, Object>) ft.get("temporalExtent");
                                    String fid = ((String) ft.get("name")).toLowerCase();

                                    ImmutableFeatureTypeConfigurationWfs3.Builder featureTypeConfigurationWfs3 = ImmutableFeatureTypeConfigurationWfs3.builder();

                                    if (wfs3ServiceData != null) {
                                        featureTypeConfigurationWfs3.from(wfs3ServiceData.getFeatureTypes()
                                                                                         .get(fid));
                                    }
                                    featureTypeConfigurationWfs3
                                            .id(fid)
                                            .label((String) ft.get("displayName"));

                                    if (temporal != null) {
                                        featureTypeConfigurationWfs3.extent(ImmutableFeatureTypeExtent.builder()
                                                                                                      .temporal(new TemporalExtent((Long) temporal.get("start"), (Integer) temporal.get("end")))
                                                                                                      .build());
                                    }

                                    return new AbstractMap.SimpleImmutableEntry<>(fid, featureTypeConfigurationWfs3.build());
                                })
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                )
                .featureProvider(
                        //TODO: providerType
                        ImmutableFeatureProviderDataWfs.builder()
                                                       .nativeCrs(new EpsgCrs((Integer) defaultCrs.get("code"), (Boolean) defaultCrs.get("longitudeFirst")))
                                                       .connectionInfo(
                                                               ImmutableConnectionInfo.builder()
                                                                                      .uri(uri)
                                                                                      .version((String) wfs.get("version"))
                                                                                      .gmlVersion((String) wfs.get("gmlVersion"))
                                                                                      .method(wfs.get("httpMethod")
                                                                                                 .equals("GET") ? ConnectionInfo.METHOD.GET : ConnectionInfo.METHOD.POST)
                                                                                      .namespaces((Map<String, String>) ((Map<String, Object>) wfs.get("nsStore"))
                                                                                              .get("namespaces"))
                                                                                      .build()
                                                       )
                                                       .mappings(
                                                               ((Map<String, Object>) service.get("featureTypes"))
                                                                       .entrySet()
                                                                       .stream()
                                                                       .map(entry -> {
                                                                           Map<String, Object> ft = (Map<String, Object>) entry.getValue();
                                                                           Map<String, Object> mappings = (Map<String, Object>) ((Map<String, Object>) ft.get("mappings")).get("mappings");
                                                                           String fid = ((String) ft.get("name")).toLowerCase();

                                                                           FeatureTypeMapping oldFt = wfs3ServiceData != null ? wfs3ServiceData.getFeatureProvider().getMappings().get(fid) : null;

                                                                           ImmutableFeatureTypeMapping newMappings = ImmutableFeatureTypeMapping.builder()
                                                                                                                                                .mappings(
                                                                                                                                                        mappings
                                                                                                                                                                .entrySet()
                                                                                                                                                                .stream()
                                                                                                                                                                .map(entry2 -> {
                                                                                                                                                                    Map<String, Object> mappings2 = (Map<String, Object>) entry2.getValue();

                                                                                                                                                                    SourcePathMapping oldSourcePathMapping = oldFt != null ? oldFt.getMappings()
                                                                                                                                                                                                                   .get(entry2.getKey()) : null;

                                                                                                                                                                    ImmutableSourcePathMapping newMappings2 = ImmutableSourcePathMapping.builder()
                                                                                                                                                                                                                                        .mappings(
                                                                                                                                                                                                                                                mappings2
                                                                                                                                                                                                                                                        .entrySet()
                                                                                                                                                                                                                                                        .stream()
                                                                                                                                                                                                                                                        .map(entry3 -> {
                                                                                                                                                                                                                                                            List<Map<String, Object>> targetMapping = (List<Map<String, Object>>) entry3.getValue();

                                                                                                                                                                                                                                                            TargetMapping oldTargetMapping = oldSourcePathMapping != null ? oldSourcePathMapping.getMappings().get(entry3.getKey()) : null;
                                                                                                                                                                                                                                                            TargetMapping newTargetMappings = createTargetMapping(entry3.getKey(), targetMapping, oldTargetMapping);


                                                                                                                                                                                                                                                            return new AbstractMap.SimpleImmutableEntry<>(entry3.getKey(), newTargetMappings);
                                                                                                                                                                                                                                                        })
                                                                                                                                                                                                                                                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                                                                                                                                                                                                                                        )
                                                                                                                                                                                                                                        .build();

                                                                                                                                                                    return new AbstractMap.SimpleImmutableEntry<>(entry2.getKey(), newMappings2);
                                                                                                                                                                })
                                                                                                                                                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                                                                                                                                                )
                                                                                                                                                .build();

                                                                           return new AbstractMap.SimpleImmutableEntry<>(fid, newMappings);
                                                                       })
                                                                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                                                       )
                                                       .build()
                )
                .build();
    }

    private TargetMapping createTargetMapping(String mimeType, List<Map<String, Object>> mappings, TargetMapping oldTargetMapping) {
        if (mappings.isEmpty()) return null;

        Map<String, Object> mapping = mappings.get(0);
        String mappingType = (String) mapping.get("mappingType");

        switch (mimeType) {
            case Wfs3GenericMapping.BASE_TYPE:
                return createGenericMapping(mappingType, mapping, (Wfs3GenericMapping) oldTargetMapping);
            case Gml2GeoJsonMappingProvider.MIME_TYPE:
                return createGeoJsonMapping(mappingType, mapping, (GeoJsonPropertyMapping) oldTargetMapping);
            case Gml2MicrodataMappingProvider.MIME_TYPE:
                return createMicrodataMapping(mappingType, mapping, (MicrodataPropertyMapping) oldTargetMapping);
            case Gml2JsonLdMappingProvider.MIME_TYPE:
                return createMicrodataMapping(mappingType, mapping, (MicrodataPropertyMapping) oldTargetMapping);
        }
        return null;
    }

    private Wfs3GenericMapping createGenericMapping(String mappingType, Map<String, Object> mapping, Wfs3GenericMapping oldTargetMapping) {
        switch (mappingType) {
            case "GENERIC_PROPERTY":
                Wfs3GenericMapping targetMapping = oldTargetMapping != null ? oldTargetMapping : new Wfs3GenericMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                if (mapping.containsKey("type"))
                    targetMapping.setType(Wfs3GenericMapping.GENERIC_TYPE.valueOf((String) mapping.get("type")));
                if (mapping.containsKey("codelist"))
                    targetMapping.setCodelist((String) mapping.get("codelist"));
                if (mapping.containsKey("format"))
                    targetMapping.setFormat((String) mapping.get("format"));
                return targetMapping;
        }
        return null;
    }

    private GeoJsonPropertyMapping createGeoJsonMapping(String mappingType, Map<String, Object> mapping, GeoJsonPropertyMapping oldTargetMapping) {
        GeoJsonPropertyMapping targetMapping = null;
        switch (mappingType) {
            case "GEO_JSON_PROPERTY":
                targetMapping = oldTargetMapping != null ? oldTargetMapping : new GeoJsonPropertyMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.valueOf((String) mapping.get("type")));
                break;
            case "GEO_JSON_GEOMETRY":
                targetMapping = oldTargetMapping != null ? oldTargetMapping : new GeoJsonGeometryMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.valueOf((String) mapping.get("type")));
                ((GeoJsonGeometryMapping) targetMapping).setGeometryType(GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.valueOf((String) mapping.get("geometryType")));
                break;
        }
        return targetMapping;
    }

    private MicrodataPropertyMapping createMicrodataMapping(String mappingType, Map<String, Object> mapping, MicrodataPropertyMapping oldTargetMapping) {
        MicrodataPropertyMapping targetMapping = null;
        switch (mappingType) {
            case "MICRODATA_PROPERTY":
                targetMapping = oldTargetMapping != null ? oldTargetMapping : new MicrodataPropertyMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                if (mapping.containsKey("type"))
                    targetMapping.setType(MicrodataMapping.MICRODATA_TYPE.valueOf((String) mapping.get("type")));
                if (mapping.containsKey("showInCollection"))
                    targetMapping.setShowInCollection((Boolean) mapping.get("showInCollection"));
                if (mapping.containsKey("itemProp"))
                    targetMapping.setItemProp((String) mapping.get("itemProp"));
                if (mapping.containsKey("itemType"))
                    targetMapping.setItemType((String) mapping.get("itemType"));
                if (mapping.containsKey("codelist"))
                    targetMapping.setCodelist((String) mapping.get("codelist"));
                if (mapping.containsKey("format"))
                    targetMapping.setFormat((String) mapping.get("format"));

                break;
            case "MICRODATA_GEOMETRY":
                targetMapping = oldTargetMapping != null ? oldTargetMapping : new MicrodataGeometryMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                targetMapping.setType(MicrodataMapping.MICRODATA_TYPE.valueOf((String) mapping.get("type")));
                ((MicrodataGeometryMapping) targetMapping).setGeometryType(MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE.valueOf((String) mapping.get("geometryType")));
                if (mapping.containsKey("showInCollection"))
                    targetMapping.setShowInCollection((Boolean) mapping.get("showInCollection"));
                break;
        }
        return targetMapping;
    }
}
