/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.legacy.OgcApiFeaturesGenericMapping;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonMapping;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonPropertyMapping;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataGeometryMapping;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataMapping;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataPropertyMapping;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.store.domain.entities.legacy.EntityRepository;
import de.ii.xtraplatform.store.domain.entities.legacy.EntityRepositoryForType;
import de.ii.xtraplatform.store.domain.legacy.KeyValueStore;
import de.ii.xtraplatform.store.domain.legacy.KeyNotFoundException;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        EntityRepositoryForType serviceRepository = new EntityRepositoryForType(entityRepository, OgcApi.TYPE);

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

                                OgcApiDataV2 datasetData = createServiceData(service, null);


                                try {
                                    Map<String, Object> serviceOverrides = jackson.getDefaultObjectMapper()
                                                                                  .readValue(serviceStore.getChildStore("#overrides#")
                                                                                                         .getValueReader(id), new TypeReference<LinkedHashMap>() {
                                                                                  });

                                    datasetData = createServiceData(serviceOverrides, datasetData);

                                } catch (KeyNotFoundException e) {
                                    //ignore
                                }


                                /*TODO try {
                                    serviceRepository.createEntity(datasetData);
                                    LOGGER.debug("MIGRATED");
                                } catch (IOException e) {
                                    LOGGER.error("ERROR", e);
                                }*/

                            } catch (Throwable e) {
                                throw new RuntimeException(e);
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

    private OgcApiDataV2 createServiceData(Map<String, Object> service,
                                           OgcApiDataV2 datasetData) throws URISyntaxException {
        Map<String, Object> wfs = (Map<String, Object>) service.get("wfsAdapter");
        Map<String, Object> defaultCrs = (Map<String, Object>) wfs.get("defaultCrs");
        String url = (String) ((Map<String, Object>) ((Map<String, Object>) wfs.get("urls"))
                .get("GetFeature")).get("GET");
        URI uri = new URI(url);

        ImmutableOgcApiDataV2.Builder builder = new ImmutableOgcApiDataV2.Builder();

        if (datasetData != null) {
            builder.from(datasetData);
        } else {
            builder
                    .id((String) service.get("id"))
                    .createdAt((Long) service.get("dateCreated"))
                    .enabled(true)
                    .serviceType("WFS3");
        }

        return builder
                .label((String) service.get("name"))
                .description((String) service.get("description"))
                .lastModified((Long) service.get("lastModified"))
                .collections(
                        ((Map<String, Object>) service.get("featureTypes"))
                                .entrySet()
                                .stream()
                                .map(entry -> {
                                    Map<String, Object> ft = (Map<String, Object>) entry.getValue();
                                    Map<String, Object> temporal = (Map<String, Object>) ft.get("temporalExtent");
                                    String fid = ((String) ft.get("name")).toLowerCase();

                                    ImmutableFeatureTypeConfigurationOgcApi.Builder featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder();

                                    if (datasetData != null) {
                                        featureTypeConfiguration.from(datasetData.getCollections()
                                                                                     .get(fid));
                                    }
                                    featureTypeConfiguration
                                            .id(fid)
                                            .label((String) ft.get("displayName"));

                                    if (temporal != null) {
                                        featureTypeConfiguration.extent(new ImmutableCollectionExtent.Builder()
                                                .temporal(new ImmutableTemporalExtent.Builder().start((Long) temporal.get("start"))
                                                                                               .end((Long) temporal.get("end"))
                                                                                               .build())
                                                .build());
                                    }

                                    return new AbstractMap.SimpleImmutableEntry<>(fid, featureTypeConfiguration.build());
                                })
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                )
                //TODO: migrate services from 1.3
                /*.featureProvider(
                        //TODO: providerType
                        new ImmutableFeatureProviderDataTransformer.Builder()
                                .nativeCrs(new EpsgCrs((Integer) defaultCrs.get("code"), (Boolean) defaultCrs.get("longitudeFirst")))
                                .connectionInfo(
                                        new ImmutableConnectionInfoWfsHttp.Builder()
                                                .uri(uri)
                                                .version((String) wfs.get("version"))
                                                .gmlVersion((String) wfs.get("gmlVersion"))
                                                .method(wfs.get("httpMethod")
                                                           .equals("GET") ? ConnectionInfoWfsHttp.METHOD.GET : ConnectionInfoWfsHttp.METHOD.POST)
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

                                                    FeatureTypeMapping oldFt = datasetData != null ? datasetData.getFeatureProvider()
                                                                                                                .getMappings()
                                                                                                                .get(fid) : null;

                                                    ImmutableFeatureTypeMapping newMappings = new ImmutableFeatureTypeMapping.Builder()
                                                            .mappings(
                                                                    mappings
                                                                            .entrySet()
                                                                            .stream()
                                                                            .map(entry2 -> {
                                                                                Map<String, Object> mappings2 = (Map<String, Object>) entry2.getValue();

                                                                                SourcePathMapping oldSourcePathMapping = oldFt != null ? oldFt.getMappings()
                                                                                                                                              .get(entry2.getKey()) : null;

                                                                                ImmutableSourcePathMapping newMappings2 = new ImmutableSourcePathMapping.Builder()
                                                                                                                                                    .mappings(
                                                                                                                                                            mappings2
                                                                                                                                                                    .entrySet()
                                                                                                                                                                    .stream()
                                                                                                                                                                    .map(entry3 -> {
                                                                                                                                                                        List<Map<String, Object>> targetMapping = (List<Map<String, Object>>) entry3.getValue();

                                                                                                                                                                        TargetMapping oldTargetMapping = oldSourcePathMapping != null ? oldSourcePathMapping.getMappings()
                                                                                                                                                                                                                                                            .get(entry3.getKey()) : null;
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
                )*/
                .build();
    }

    private TargetMapping createTargetMapping(String mimeType, List<Map<String, Object>> mappings,
                                              TargetMapping oldTargetMapping) {
        if (mappings.isEmpty()) return null;

        Map<String, Object> mapping = mappings.get(0);
        String mappingType = (String) mapping.get("mappingType");

        switch (mimeType) {
            case "general":
                return createGenericMapping(mappingType, mapping, (OgcApiFeaturesGenericMapping) oldTargetMapping);
            case "application/geo+json":
                return createGeoJsonMapping(mappingType, mapping, (GeoJsonPropertyMapping) oldTargetMapping);
            case "text/html":
                return createMicrodataMapping(mappingType, mapping, (MicrodataPropertyMapping) oldTargetMapping);
        }
        return null;
    }

    private OgcApiFeaturesGenericMapping createGenericMapping(String mappingType, Map<String, Object> mapping,
                                                              OgcApiFeaturesGenericMapping oldTargetMapping) {
        switch (mappingType) {
            case "GENERIC_PROPERTY":
                OgcApiFeaturesGenericMapping targetMapping = oldTargetMapping != null ? oldTargetMapping : new OgcApiFeaturesGenericMapping();
                targetMapping.setEnabled((Boolean) mapping.get("enabled"));
                targetMapping.setFilterable((Boolean) mapping.get("filterable"));
                targetMapping.setName((String) mapping.get("name"));
                if (mapping.containsKey("type"))
                    targetMapping.setType(OgcApiFeaturesGenericMapping.GENERIC_TYPE.valueOf((String) mapping.get("type")));
                if (mapping.containsKey("codelist"))
                    targetMapping.setCodelist((String) mapping.get("codelist"));
                if (mapping.containsKey("format"))
                    targetMapping.setFormat((String) mapping.get("format"));
                return targetMapping;
        }
        return null;
    }

    private GeoJsonPropertyMapping createGeoJsonMapping(String mappingType, Map<String, Object> mapping,
                                                        GeoJsonPropertyMapping oldTargetMapping) {
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

    private MicrodataPropertyMapping createMicrodataMapping(String mappingType, Map<String, Object> mapping,
                                                            MicrodataPropertyMapping oldTargetMapping) {
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
