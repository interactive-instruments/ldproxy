/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.migrations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.legacy.OgcApiFeaturesGenericMapping;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.ImmutableCrsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV1;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV1;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTypeMapping2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeatureTypeMapping2;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonMapping;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonPropertyMapping;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.ImmutableGeoJsonLdConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataMapping;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataPropertyMapping;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.GeoJsonLdConfiguration;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureActionTrigger;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerRemove.Condition;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityMigration;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiDataV2.SERVICE_TYPE)
})
@Instantiate
@SuppressWarnings("deprecation")
public class OgcApiApiMigrationV1V2 implements EntityMigration<OgcApiDataV1, OgcApiDataV2> {

    // TODO need to update

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiMigrationV1V2.class);

    @Override
    public long getSourceVersion() {
        return 1;
    }

    @Override
    public long getTargetVersion() {
        return 2;
    }

    @Override
    public EntityDataBuilder<OgcApiDataV1> getDataBuilder() {
        return new ImmutableOgcApiDataV1.Builder();
    }

    @Override
    public OgcApiDataV2 migrate(OgcApiDataV1 entityData) {

        List<ExtensionConfiguration> extensions = entityData.getExtensions()
                                                            .stream()
                                                            .map(extensionConfiguration -> {
                                                                if (extensionConfiguration instanceof FeaturesCoreConfiguration) {
                                                                    return new ImmutableFeaturesCoreConfiguration.Builder()
                                                                            .from(extensionConfiguration)
                                                                            .featureProvider(entityData.getId())
                                                                            .build();
                                                                }
                                                                if (extensionConfiguration instanceof CrsConfiguration) {
                                                                    return new ImmutableCrsConfiguration.Builder()
                                                                            .from(extensionConfiguration)
                                                                            .additionalCrs(entityData.getAdditionalCrs())
                                                                            .build();
                                                                }

                                                                return extensionConfiguration;
                                                            })
                                                            .collect(Collectors.toList());

        Map<String, FeatureTypeConfigurationOgcApi> collections = entityData.getFeatureTypes()
                                                                            .entrySet()
                                                                            .stream()
                                                                            .filter(entry -> entityData.getFeatureProvider()
                                                                                                       .getMappings()
                                                                                                       .containsKey(entry.getValue()
                                                                                                                         .getId()))
                                                                            .map(entry -> {

                                                                                FeatureTypeConfigurationOgcApi collection = entry.getValue();

                                                                                ImmutableFeatureTypeConfigurationOgcApi.Builder newCollection = new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(collection);

                                                                                if (isFeatureTypeDisabled(entityData.getFeatureProvider()
                                                                                                                    .getMappings()
                                                                                                                    .get(collection.getId()), entityData.getFeatureProvider()
                                                                                                                                                        .getProviderType())) {
                                                                                    newCollection.enabled(false);
                                                                                }

                                                                                Optional<FeaturesCoreConfiguration> coreConfiguration = collection.getExtension(FeaturesCoreConfiguration.class);

                                                                                ImmutableFeaturesCoreConfiguration.Builder newCoreConfiguration = new ImmutableFeaturesCoreConfiguration.Builder();

                                                                                coreConfiguration.ifPresent(newCoreConfiguration::from);

                                                                                FeaturesCollectionQueryables queryables = getQueryables(entityData.getFeatureProvider()
                                                                                                                                                  .getMappings()
                                                                                                                                                  .get(collection.getId()));
                                                                                newCoreConfiguration.featureType(collection.getId())
                                                                                                    .queryables(queryables);

                                                                                Map<String, FeatureTypeMapping2> coreTransformations = getCoreTransformations(entityData.getFeatureProvider()
                                                                                                                                                                        .getMappings()
                                                                                                                                                                        .get(collection.getId()));

                                                                                newCoreConfiguration.transformations(coreTransformations);


                                                                                Optional<GeoJsonConfiguration> geoJsonConfiguration = collection.getExtension(GeoJsonConfiguration.class);

                                                                                ImmutableGeoJsonConfiguration.Builder newGeoJsonConfiguration = new ImmutableGeoJsonConfiguration.Builder();

                                                                                geoJsonConfiguration.ifPresent(newGeoJsonConfiguration::from);

                                                                                Optional<GeoJsonLdConfiguration> geoJsonLdConfiguration = collection.getExtension(GeoJsonLdConfiguration.class);

                                                                                ImmutableGeoJsonLdConfiguration.Builder newGeoJsonLdConfiguration = new ImmutableGeoJsonLdConfiguration.Builder();

                                                                                geoJsonLdConfiguration.ifPresent(newGeoJsonLdConfiguration::from);

                                                                                getJsonLdOptions(entityData.getFeatureProvider()
                                                                                                           .getMappings()
                                                                                                           .get(collection.getId())).ifPresent(newGeoJsonLdConfiguration::from);

                                                                                Optional<FeaturesHtmlConfiguration> htmlConfiguration = collection.getExtension(FeaturesHtmlConfiguration.class);

                                                                                ImmutableFeaturesHtmlConfiguration.Builder newHtmlConfiguration = new ImmutableFeaturesHtmlConfiguration.Builder();

                                                                                htmlConfiguration.ifPresent(newHtmlConfiguration::from);


                                                                                Map<String, FeatureTypeMapping2> htmlTransformations = getHtmlTransformations(entityData.getFeatureProvider()
                                                                                                                                                                        .getMappings()
                                                                                                                                                                        .get(collection.getId()));

                                                                                newHtmlConfiguration.transformations(htmlTransformations);

                                                                                Optional<String> htmlName = getHtmlName(entityData.getFeatureProvider()
                                                                                                                                  .getMappings()
                                                                                                                                  .get(collection.getId()));

                                                                                htmlName.ifPresent(htmlName1 -> {
                                                                                    final String[] itemLabelFormat = {htmlName1};

                                                                                    htmlTransformations.forEach((key, value) -> {
                                                                                        if (value.getRename()
                                                                                                 .isPresent()) {
                                                                                            String rename = String.format("{{%s}}", value.getRename()
                                                                                                                                         .get());
                                                                                            if (itemLabelFormat[0].contains(rename)) {
                                                                                                itemLabelFormat[0] = itemLabelFormat[0].replace(rename, String.format("{{%s}}", key));
                                                                                            }
                                                                                        }
                                                                                    });

                                                                                    newHtmlConfiguration.itemLabelFormat(itemLabelFormat[0]);
                                                                                });


                                                                                List<ExtensionConfiguration> newExtensions = collection.getExtensions()
                                                                                                                                       .stream()
                                                                                                                                       .map(extension -> {

                                                                                                                                           if (extension instanceof FeaturesCoreConfiguration) {
                                                                                                                                               return newCoreConfiguration.build();
                                                                                                                                           }
                                                                                                                                           if (extension instanceof GeoJsonConfiguration) {
                                                                                                                                               return newGeoJsonConfiguration.build();
                                                                                                                                           }
                                                                                                                                           if (extension instanceof GeoJsonLdConfiguration) {
                                                                                                                                               return newGeoJsonLdConfiguration.build();
                                                                                                                                           }
                                                                                                                                           if (extension instanceof FeaturesHtmlConfiguration) {
                                                                                                                                               return newHtmlConfiguration.build();
                                                                                                                                           }

                                                                                                                                           return extension;
                                                                                                                                       })
                                                                                                                                       .collect(Collectors.toList());

                                                                                newCollection.extensions(newExtensions);

                                                                                if (newExtensions.stream()
                                                                                                 .noneMatch(extension -> extension instanceof FeaturesCoreConfiguration)) {
                                                                                    newCollection.addExtensions(newCoreConfiguration.build());
                                                                                }
                                                                                if (newExtensions.stream()
                                                                                                 .noneMatch(extension -> extension instanceof GeoJsonConfiguration)) {
                                                                                    newCollection.addExtensions(newGeoJsonConfiguration.build());
                                                                                }
                                                                                if (newExtensions.stream()
                                                                                                 .noneMatch(extension -> extension instanceof GeoJsonLdConfiguration)) {
                                                                                    newCollection.addExtensions(newGeoJsonLdConfiguration.build());
                                                                                }
                                                                                if (newExtensions.stream()
                                                                                                 .noneMatch(extension -> extension instanceof FeaturesHtmlConfiguration)) {
                                                                                    newCollection.addExtensions(newHtmlConfiguration.build());
                                                                                }

                                                                                return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), newCollection.build());
                                                                            })
                                                                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        OgcApiDataV2 migrated = new ImmutableOgcApiDataV2.Builder().from((ServiceData) entityData)
                                                                   .serviceType("OGC_API")
                                                                   .entityStorageVersion(getTargetVersion())
                                                                   .metadata(Optional.ofNullable(entityData.getMetadata()))
                                                                   .collections(collections)
                                                                   .extensions(extensions)
                                                                   .build();

        return migrated;
    }

    @Override
    public Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, OgcApiDataV1 entityData) {

        FeatureProviderDataTransformer featureProvider = entityData.getFeatureProvider();
        String providerType = featureProvider.getProviderType();

        if (Objects.equals(providerType, "PGIS") || Objects.equals(providerType, "WFS")) {

            Map<String, FeatureType> featureTypes = featureProvider.getMappings()
                                                                   .entrySet()
                                                                   .stream()
                                                                   .map(entry -> {

                                                                       ImmutableFeatureType.Builder featureType = new ImmutableFeatureType.Builder()
                                                                               .name(entry.getKey());
                                                                       String[] featureTypeName = new String[1];
                                                                       boolean[] isFirst = {true};
                                                                       entry.getValue()
                                                                            .getMappings()
                                                                            .entrySet()
                                                                            .stream()
                                                                            .sorted(OgcApiApiMigrationV1V2::compareSortPriority)
                                                                            .forEach(entry2 -> {
                                                                                String path = entry2.getKey();
                                                                                SourcePathMapping mappings = entry2.getValue();
                                                                                boolean isFeatureTypeMapping = isFeatureTypeMapping(providerType, path, isFirst[0]);
                                                                                isFirst[0] = false;

                                                                                if (isFeatureTypeMapping) {
                                                                                    featureTypeName[0] = path;
                                                                                    return;
                                                                                }

                                                                                Optional<OgcApiFeaturesGenericMapping> general;
                                                                                try {
                                                                                    general = Optional.ofNullable((OgcApiFeaturesGenericMapping) mappings.getMappings()
                                                                                                                                                         .get("general"));
                                                                                } catch (Throwable e) {
                                                                                    general = Optional.empty();
                                                                                }
                                                                                //GeoJsonGeometryMapping
                                                                                Optional<GeoJsonPropertyMapping> json;
                                                                                try {
                                                                                    json = Optional.ofNullable((GeoJsonPropertyMapping) mappings.getMappings()
                                                                                                                                                .get("application/geo+json"));
                                                                                } catch (Throwable e) {
                                                                                    json = Optional.empty();
                                                                                }

                                                                                //MicrodataGeometryMapping
                                                                                Optional<MicrodataPropertyMapping> html;
                                                                                try {
                                                                                    html = Optional.ofNullable((MicrodataPropertyMapping) mappings.getMappings()
                                                                                                                                                  .get("text/html"));
                                                                                } catch (Throwable e) {
                                                                                    html = Optional.empty();
                                                                                }

                                                                                if (!isFirst[0] && general.isPresent()) {
                                                                                    String name = json.flatMap(jsonMapping -> Optional.ofNullable(jsonMapping.getName()))
                                                                                                      .orElse(Optional.ofNullable(general.get()
                                                                                                                                         .getName())
                                                                                                                      .orElse("TODO"));
                                                                                    OgcApiFeaturesGenericMapping.GENERIC_TYPE genericType = Optional.ofNullable(general.get()
                                                                                                                                                                       .getType())
                                                                                                                                                    .orElse(OgcApiFeaturesGenericMapping.GENERIC_TYPE.NONE);
                                                                                    GeoJsonMapping.GEO_JSON_TYPE jsonType = json.flatMap(jsonMapping -> Optional.ofNullable(jsonMapping.getType()))
                                                                                                                                .orElse(GeoJsonMapping.GEO_JSON_TYPE.STRING);

                                                                                    FeatureProperty.Type type = FeatureProperty.Type.STRING;
                                                                                    Optional<FeatureProperty.Role> role = Optional.empty();
                                                                                    switch (genericType) {

                                                                                        case TEMPORAL:
                                                                                            type = FeatureProperty.Type.DATETIME;
                                                                                            break;
                                                                                        case SPATIAL:
                                                                                            type = FeatureProperty.Type.GEOMETRY;
                                                                                            break;
                                                                                        case ID:
                                                                                            role = Optional.of(FeatureProperty.Role.ID);
                                                                                        case VALUE:
                                                                                        case REFERENCE:
                                                                                        case REFERENCE_EMBEDDED:
                                                                                        case NONE:
                                                                                            switch (jsonType) {

                                                                                                case INTEGER:
                                                                                                    type = FeatureProperty.Type.INTEGER;
                                                                                                    break;
                                                                                                case NUMBER:
                                                                                                case DOUBLE:
                                                                                                    type = FeatureProperty.Type.FLOAT;
                                                                                                    break;
                                                                                                case GEOMETRY:
                                                                                                    type = FeatureProperty.Type.GEOMETRY;
                                                                                                    break;
                                                                                                case BOOLEAN:
                                                                                                    type = FeatureProperty.Type.BOOLEAN;
                                                                                                    break;
                                                                                                case ID:
                                                                                                case STRING:
                                                                                                case NONE:
                                                                                                    type = FeatureProperty.Type.STRING;
                                                                                            }
                                                                                    }

                                                                                    FeatureProperty featureProperty = new ImmutableFeatureProperty.Builder()
                                                                                            .name(name)
                                                                                            .path(adjustPath(path, featureTypeName[0], providerType, featureProvider.getConnectionInfo()))
                                                                                            .type(type)
                                                                                            .role(role)
                                                                                            .build();

                                                                                    featureType.putProperties(name, featureProperty);
                                                                                }


                                                                            });

                                                                       return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), featureType.build());
                                                                   })
                                                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            ImmutableFeatureProviderDataV1.Builder featureProviderData = new ImmutableFeatureProviderDataV1.Builder()
                    .id(entityData.getId())
                    .providerType("FEATURE")
                    .nativeCrs(featureProvider.getNativeCrs())
                    .types(featureTypes);

            if (Objects.equals(providerType, "PGIS")) {
                featureProviderData.featureProviderType("SQL")
                                   .connectionInfo(new ImmutableConnectionInfoSql.Builder()
                                           .from(featureProvider.getConnectionInfo())
                                           .connectorType("SLICK")
                                           .dialect(ConnectionInfoSql.Dialect.PGIS)
                                           .computeNumberMatched(featureProvider.computeNumberMatched())
                                           .triggers(Optional.ofNullable(featureProvider.getTrigger()).map(o -> (FeatureActionTrigger)o))
                                           .build());
            } else if (Objects.equals(providerType, "WFS")) {
                featureProviderData.featureProviderType("WFS")
                                   .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                           .from(featureProvider.getConnectionInfo())
                                           .connectorType("HTTP")
                                           .build());
            }

            List<String> path = ImmutableList.<String>builder()
                    .add("providers")
                    .addAll(identifier.path()
                                      .subList(1, identifier.path()
                                                            .size()))
                    .build();
            Identifier newIdentifier = Identifier.from(identifier.id(), path.toArray(new String[]{}));

            return ImmutableMap.of(newIdentifier, featureProviderData.build());
        }

        LOGGER.warn("Could not migrate feature provider for service '{}', providerType '{}' is not supported yet.", entityData.getId(), providerType);

        return ImmutableMap.of();

    }

    private String adjustPath(String path, String featureTypeName, String providerType, ConnectionInfo connectionInfo) {

        if (Objects.equals(providerType, "WFS") && connectionInfo instanceof ConnectionInfoWfsHttp) {
            Map<String, String> namespaces = ((ConnectionInfoWfsHttp) connectionInfo).getNamespaces();

            String resolvedPath = String.format("/%s/%s", featureTypeName, path);

            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                String prefix = entry.getKey();
                String uri = entry.getValue();
                resolvedPath = resolvedPath.replaceAll(uri, prefix);
            }

            return resolvedPath;
        }

        return path;
    }

    private FeaturesCollectionQueryables getQueryables(
            FeatureTypeMapping featureTypeMapping) {
        ImmutableFeaturesCollectionQueryables.Builder builder = new ImmutableFeaturesCollectionQueryables.Builder();

        List<OgcApiFeaturesGenericMapping> filterables = featureTypeMapping.getMappings()
                                                                           .values()
                                                                           .stream()
                                                                           .sorted(OgcApiApiMigrationV1V2::compareSortPriority)
                                                                           .filter(sourcePathMapping -> sourcePathMapping.hasMappingForType("general"))
                                                                           .map(sourcePathMapping -> (OgcApiFeaturesGenericMapping) sourcePathMapping.getMappingForType("general"))
                                                                           .filter(OgcApiFeaturesGenericMapping::isFilterable)
                                                                           .collect(Collectors.toList());

        List<String> spatial = filterables.stream()
                                          .filter(mapping -> mapping.getType() == OgcApiFeaturesGenericMapping.GENERIC_TYPE.SPATIAL)
                                          .map(OgcApiFeaturesGenericMapping::getName)
                                          .collect(Collectors.toList());

        List<String> temporal = filterables.stream()
                                           .filter(mapping -> mapping.getType() == OgcApiFeaturesGenericMapping.GENERIC_TYPE.TEMPORAL)
                                           .map(OgcApiFeaturesGenericMapping::getName)
                                           .collect(Collectors.toList());

        List<String> other = filterables.stream()
                                        .filter(mapping -> mapping.getType() != OgcApiFeaturesGenericMapping.GENERIC_TYPE.SPATIAL && mapping.getType() != OgcApiFeaturesGenericMapping.GENERIC_TYPE.TEMPORAL)
                                        .map(OgcApiFeaturesGenericMapping::getName)
                                        .collect(Collectors.toList());

        return builder.spatial(spatial)
                      .temporal(temporal)
                      .other(other)
                      .build();
    }

    private Optional<String> getHtmlName(FeatureTypeMapping featureTypeMapping) {

        final String[] name = {null};

        featureTypeMapping.getMappings()
                          .values()
                          .stream()
                          .filter(sourcePathMapping -> sourcePathMapping.hasMappingForType("general") && sourcePathMapping.hasMappingForType("text/html"))
                          .forEach(sourcePathMapping -> {
                              OgcApiFeaturesGenericMapping general = (OgcApiFeaturesGenericMapping) sourcePathMapping.getMappingForType("general");
                              MicrodataPropertyMapping html = (MicrodataPropertyMapping) sourcePathMapping.getMappingForType("text/html");

                              if (Objects.isNull(general.getName()) && Objects.nonNull(html.getName())) {

                                  name[0] = html.getName();
                              }
                          });

        return Optional.ofNullable(name[0]);
    }

    private Map<String, FeatureTypeMapping2> getHtmlTransformations(FeatureTypeMapping featureTypeMapping) {

        Map<String, FeatureTypeMapping2> transformations = new LinkedHashMap<>();

        featureTypeMapping.getMappings()
                          .values()
                          .stream()
                          .filter(sourcePathMapping -> sourcePathMapping.hasMappingForType("general") && sourcePathMapping.hasMappingForType("text/html"))
                          .sorted(OgcApiApiMigrationV1V2::compareSortPriority)
                          .forEach(sourcePathMapping -> {
                              OgcApiFeaturesGenericMapping general = (OgcApiFeaturesGenericMapping) sourcePathMapping.getMappingForType("general");
                              MicrodataPropertyMapping html = (MicrodataPropertyMapping) sourcePathMapping.getMappingForType("text/html");

                              if (Objects.isNull(general.getName()) /*&& Objects.nonNull(html.getName())*/) {
                                  return;
                              }

                              //TODO: transactions? not yet in geoval
                              //TODO: mainWritePathPattern in FileSystemEvents -> DONE???

                              //TODO: connectorType + mappingStatus @ featureProvider
                              //TODO: api buildingBlocks as Object? i think does not work with deser
                              //TODO: ConnectionInfoV1 with custom deser
                              //TODO: foto[foto].hauptfoto -> foto[].hauptfoto

                              ImmutableFeatureTypeMapping2.Builder builder = new ImmutableFeatureTypeMapping2.Builder();
                              boolean hasTransformations = false;

                              if (Objects.isNull(html.isShowInCollection()) || !html.isShowInCollection()) {
                                  builder.remove("OVERVIEW");
                                  hasTransformations = true;
                              }

                              if (general.getType() != OgcApiFeaturesGenericMapping.GENERIC_TYPE.SPATIAL) {
                                  if (Objects.nonNull(html.getName()) && !Objects.equals(general.getName(), html.getName())) {
                                      builder.rename(html.getName());
                                      hasTransformations = true;
                                  }
                                  if (Objects.nonNull(html.getCodelist())) {
                                      builder.codelist(html.getCodelist());
                                      hasTransformations = true;
                                  }
                                  if (Objects.nonNull(html.getFormat())) {
                                      if (html.getType() == MicrodataMapping.MICRODATA_TYPE.DATE) {
                                          builder.dateFormat(html.getFormat());
                                      } else {
                                          builder.stringFormat(html.getFormat());
                                      }
                                      hasTransformations = true;
                                  }
                              }

                              if (hasTransformations) {
                                  transformations.put(general.getName()
                                                             .replaceAll("\\[[^\\]]+?\\]", "[]"), builder.build());
                              }
                          });

        return transformations;
    }

    private Optional<GeoJsonLdConfiguration> getJsonLdOptions(FeatureTypeMapping featureTypeMapping) {

        ImmutableGeoJsonLdConfiguration.Builder builder = new ImmutableGeoJsonLdConfiguration.Builder();
        boolean isJsonLd = false;

        for (Map.Entry<String, SourcePathMapping> entry : featureTypeMapping.getMappings()
                                                                            .entrySet()) {
            if (entry.getValue()
                     .hasMappingForType("application/geo+json")) {
                GeoJsonPropertyMapping geoJson = (GeoJsonPropertyMapping) entry.getValue()
                                                                               .getMappingForType("application/geo+json");
                if (Objects.nonNull(geoJson.getLdContext())) {
                    isJsonLd = true;
                    builder.enabled(true)
                           .context(geoJson.getLdContext());
                }
                if (Objects.nonNull(geoJson.getLdType())) {
                    builder.types(geoJson.getLdType());
                }
                if (Objects.nonNull(geoJson.getIdTemplate())) {
                    builder.idTemplate(geoJson.getIdTemplate());
                }
            }
        }

        return isJsonLd ? Optional.of(builder.build()) : Optional.empty();
    }

    private Map<String, FeatureTypeMapping2> getCoreTransformations(FeatureTypeMapping featureTypeMapping) {

        Map<String, FeatureTypeMapping2> transformations = new LinkedHashMap<>();

        featureTypeMapping.getMappings()
                          .values()
                          .stream()
                          .filter(sourcePathMapping -> sourcePathMapping.hasMappingForType("general"))
                          .sorted(OgcApiApiMigrationV1V2::compareSortPriority)
                          .map(sourcePathMapping -> (OgcApiFeaturesGenericMapping) sourcePathMapping.getMappingForType("general"))
                          .filter(general -> Objects.nonNull(general.getName()))
                          .forEach(general -> {

                              ImmutableFeatureTypeMapping2.Builder builder = new ImmutableFeatureTypeMapping2.Builder();
                              boolean hasTransformations = false;

                              if (general.getType() != OgcApiFeaturesGenericMapping.GENERIC_TYPE.SPATIAL) {
                                  if (Objects.nonNull(general.getCodelist())) {
                                      builder.codelist(general.getCodelist());
                                      hasTransformations = true;
                                  }
                                  if (Objects.nonNull(general.getFormat())) {
                                      if (general.getType() == OgcApiFeaturesGenericMapping.GENERIC_TYPE.TEMPORAL) {
                                          builder.dateFormat(general.getFormat());
                                      } else {
                                          builder.stringFormat(general.getFormat());
                                      }
                                      hasTransformations = true;
                                  }
                                  if (!general.isEnabled()) {
                                      builder.remove(Condition.ALWAYS.toString());
                                      hasTransformations = true;
                                  }
                              }

                              if (hasTransformations) {
                                  transformations.put(general.getName()
                                                             .replaceAll("\\[[^\\]]+?\\]", "[]"), builder.build());
                              }
                          });

        return transformations;
    }

    private boolean isFeatureTypeDisabled(FeatureTypeMapping featureTypeMapping, String providerType) {

        boolean isFirst = true;
        for (Map.Entry<String, SourcePathMapping> entry : featureTypeMapping.getMappings()
                                                                            .entrySet()
                                                                            .stream()
                                                                            .sorted(OgcApiApiMigrationV1V2::compareSortPriority)
                                                                            .collect(Collectors.toList())) {
            if (isFeatureTypeMapping(providerType, entry.getKey(), isFirst) && entry.getValue()
                                                                           .hasMappingForType("general")) {
                OgcApiFeaturesGenericMapping general = (OgcApiFeaturesGenericMapping) entry.getValue()
                                                                                           .getMappingForType("general");
                if (Objects.nonNull(general) && !general.isEnabled()) {
                    return true;
                }
            }
            isFirst = false;
        }

        return false;
    }

    private static int compareSortPriority(Map.Entry<String, SourcePathMapping> stringSourcePathMappingEntry,
                                           Map.Entry<String, SourcePathMapping> stringSourcePathMappingEntry1) {
        return compareSortPriority(stringSourcePathMappingEntry.getValue(), stringSourcePathMappingEntry1.getValue());
    }

    private static int compareSortPriority(SourcePathMapping sourcePathMapping, SourcePathMapping sourcePathMapping2) {
        OgcApiFeaturesGenericMapping general = (OgcApiFeaturesGenericMapping) sourcePathMapping.getMappingForType("general");
        OgcApiFeaturesGenericMapping general2 = (OgcApiFeaturesGenericMapping) sourcePathMapping2.getMappingForType("general");
        if (general.getSortPriority() == null) {
            return 1;
        }

        if (general2.getSortPriority() == null) {
            return -1;
        }

        return general.getSortPriority() - general2.getSortPriority();
    }

    private boolean isFeatureTypeMapping(String providerType, String path, boolean isFirst) {

        if (Objects.equals(providerType, "PGIS")) {
            return path.indexOf("/") == path.lastIndexOf("/");
        } else if (Objects.equals(providerType, "WFS")) {
            return isFirst;// Character.isUpperCase(path.charAt(path.lastIndexOf(":") + 1));
        }

        return false;
    }


}
