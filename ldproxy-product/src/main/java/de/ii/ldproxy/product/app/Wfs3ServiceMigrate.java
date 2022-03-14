/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.product.app;

/**
 * @author zahnen
 */
public class Wfs3ServiceMigrate {
/*
    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceMigrate.class);
    private static final String[] PATH = {"ldproxy-services"};
    private final EntityRepository entityRepository;
    private final KeyValueStore rootConfigStore;
    private final Jackson jackson;
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public Wfs3ServiceMigrate(@Requires EntityRepository entityRepository, @Requires KeyValueStore rootConfigStore,
                              @Requires Jackson jackson) {
        this.entityRepository = entityRepository;
        this.rootConfigStore = rootConfigStore;
        this.jackson = jackson;
    }

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



                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }


                        });


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
    */
}
