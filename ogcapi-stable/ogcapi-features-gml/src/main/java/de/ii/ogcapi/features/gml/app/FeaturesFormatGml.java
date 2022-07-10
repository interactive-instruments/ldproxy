/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration.Conformance;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.ogcapi.features.gml.domain.GmlWriterRegistry;
import de.ii.ogcapi.features.gml.domain.ImmutableFeatureTransformationContextGml;
import de.ii.ogcapi.features.gml.domain.ImmutableFeatureTransformationContextGmlUpgrade;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class FeaturesFormatGml implements ConformanceClass, FeatureFormatExtension {

  private static final Map<String, String> STANDARD_NAMESPACES =
      ImmutableMap.of(
          "gml",
          "http://www.opengis.net/gml/3.2",
          "xlink",
          "http://www.w3.org/1999/xlink",
          "xml",
          "http://www.w3.org/XML/1998/namespace",
          "xsi",
          "http://www.w3.org/2001/XMLSchema-instance",
          "sf",
          "http://www.opengis.net/ogcapi-features-1/1.0/sf",
          "wfs",
          "http://www.opengis.net/wfs/2.0");

  private static final Map<String, String> STANDARD_SCHEMA_LOCATIONS =
      ImmutableMap.of(
          "gml",
          "http://schemas.opengis.net/gml/3.2.1/gml.xsd",
          "xlink",
          "http://www.w3.org/1999/xlink.xsd",
          "xml",
          "http://www.w3.org/2001/xml.xsd",
          "sf",
          "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core-sf.xsd",
          "wfs",
          "http://schemas.opengis.net/wfs/2.0/wfs.xsd");

  private static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "gml+xml"))
          .label("GML")
          .parameter("xml")
          .build();
  private static final ApiMediaType MEDIA_TYPE_GMLSF0 =
      new ImmutableApiMediaType.Builder()
          .type(
              new MediaType(
                  "application",
                  "gml+xml",
                  ImmutableMap.of(
                      "version",
                      "3.2",
                      "profile",
                      "http://www.opengis.net/def/profile/ogc/2.0/gml-sf0")))
          .label("GML")
          .parameter("xml")
          .build();
  private static final ApiMediaType MEDIA_TYPE_GMLSF2 =
      new ImmutableApiMediaType.Builder()
          .type(
              new MediaType(
                  "application",
                  "gml+xml",
                  ImmutableMap.of(
                      "version",
                      "3.2",
                      "profile",
                      "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
          .label("GML")
          .parameter("xml")
          .build();
  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "xml"))
          .label("XML")
          .parameter("xml")
          .build();

  private static final Map<Conformance, ApiMediaType> MEDIA_TYPE_MAP =
      ImmutableMap.of(
          Conformance.NONE, MEDIA_TYPE,
          Conformance.GMLSF0, MEDIA_TYPE_GMLSF0,
          Conformance.GMLSF2, MEDIA_TYPE_GMLSF2);

  private final FeaturesCoreProviders providers;
  private final EntityRegistry entityRegistry;
  private final FeaturesCoreValidation featuresCoreValidator;
  private final GmlWriterRegistry gmlWriterRegistry;

  @Inject
  public FeaturesFormatGml(
      FeaturesCoreProviders providers,
      EntityRegistry entityRegistry,
      FeaturesCoreValidation featuresCoreValidator,
      GmlWriterRegistry gmlWriterRegistry) {
    this.providers = providers;
    this.entityRegistry = entityRegistry;
    this.featuresCoreValidator = featuresCoreValidator;
    this.gmlWriterRegistry = gmlWriterRegistry;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    switch (getConformance(apiData)) {
      case GMLSF0:
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf0");
      case GMLSF2:
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2");
      default:
      case NONE:
        return ImmutableList.of();
    }
  }

  private Conformance getConformance(OgcApiDataV2 apiData) {
    Set<Conformance> conformance =
        apiData.getCollections().values().stream()
            .filter(
                collectionData ->
                    collectionData
                        .getExtension(GmlConfiguration.class)
                        .map(ExtensionConfiguration::isEnabled)
                        .orElse(false))
            .map(
                collectionData ->
                    getConformance(collectionData.getExtension(GmlConfiguration.class)))
            .collect(Collectors.toUnmodifiableSet());
    if (!conformance.contains(Conformance.NONE)) {
      if (conformance.contains(Conformance.GMLSF0)) {
        return Conformance.GMLSF0;
      } else if (conformance.contains(Conformance.GMLSF2)) {
        return Conformance.GMLSF2;
      }
    }
    return Conformance.NONE;
  }

  private Conformance getConformance(Optional<GmlConfiguration> configuration) {
    return configuration
        .filter(c -> !"sf:FeatureCollection".equals(c.getFeatureCollectionElementName()))
        .filter(c -> !"sf:featureMember".equals(c.getFeatureMemberElementName()))
        .map(c -> Conformance.NONE)
        .orElse(configuration.map(GmlConfiguration::getConformance).orElse(Conformance.NONE));
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new ObjectSchema())
        .schemaRef("#/components/schemas/anyObject")
        .ogcApiMediaType(MEDIA_TYPE_MAP.get(getConformance(apiData)))
        .build();
  }

  @Override
  public ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GmlConfiguration.class;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) return ValidationResult.of();

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(api.getData());

    // get GML configurations to process
    Map<String, GmlConfiguration> gmlConfigurationMap =
        api.getData().getCollections().entrySet().stream()
            .map(
                entry -> {
                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  final GmlConfiguration config =
                      collectionData.getExtension(GmlConfiguration.class).orElse(null);
                  if (Objects.isNull(config)) return null;
                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                })
            .filter(Objects::nonNull)
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<String, Collection<String>> keyMap =
        gmlConfigurationMap.entrySet().stream()
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().getTransformations().keySet()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    for (Map.Entry<String, Collection<String>> stringCollectionEntry :
        featuresCoreValidator.getInvalidPropertyKeys(keyMap, featureSchemas).entrySet()) {
      for (String property : stringCollectionEntry.getValue()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.",
                property, stringCollectionEntry.getKey()));
      }
    }

    Set<String> codelists =
        entityRegistry.getEntitiesForType(Codelist.class).stream()
            .map(Codelist::getId)
            .collect(Collectors.toUnmodifiableSet());
    for (Map.Entry<String, GmlConfiguration> entry : gmlConfigurationMap.entrySet()) {
      String collectionId = entry.getKey();
      for (Map.Entry<String, List<PropertyTransformation>> entry2 :
          entry.getValue().getTransformations().entrySet()) {
        String property = entry2.getKey();
        for (PropertyTransformation transformation : entry2.getValue()) {
          builder = transformation.validate(builder, collectionId, property, codelists);
        }
      }
    }

    return builder.build();
  }

  @Override
  public boolean canPassThroughFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoderPassThrough(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.of(
        new FeatureEncoderGmlUpgrade(
            ImmutableFeatureTransformationContextGmlUpgrade.builder()
                .from(transformationContext)
                .namespaces(
                    ((ConnectionInfoWfsHttp)
                            ((WithConnectionInfo<?>)
                                    providers
                                        .getFeatureProviderOrThrow(
                                            transformationContext.getApiData())
                                        .getData())
                                .getConnectionInfo())
                        .getNamespaces())
                .build()));
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    ImmutableSortedSet<GmlWriter> gmlWriters =
        gmlWriterRegistry.getWriters().stream()
            .map(GmlWriter::create)
            .collect(
                ImmutableSortedSet.toImmutableSortedSet(
                    Comparator.comparingInt(GmlWriter::getSortPriority)));

    @SuppressWarnings("ConstantConditions")
    GmlConfiguration config =
        transformationContext
            .getApiData()
            .getCollections()
            .get(transformationContext.getCollectionId())
            .getExtension(GmlConfiguration.class)
            .orElseThrow();
    ImmutableFeatureTransformationContextGml transformationContextGml =
        ImmutableFeatureTransformationContextGml.builder()
            .from(transformationContext)
            .putAllNamespaces(config.getApplicationNamespaces())
            .putAllNamespaces(STANDARD_NAMESPACES)
            .defaultNamespace(Optional.ofNullable(config.getDefaultNamespace()))
            .putAllSchemaLocations(config.getSchemaLocations())
            .putAllSchemaLocations(STANDARD_SCHEMA_LOCATIONS)
            .objectTypeNamespaces(config.getObjectTypeNamespaces())
            .variableObjectElementNames(config.getVariableObjectElementNames())
            .featureCollectionElementName(
                Optional.ofNullable(config.getFeatureCollectionElementName()))
            .featureMemberElementName(Optional.ofNullable(config.getFeatureMemberElementName()))
            .supportsStandardResponseParameters(
                Objects.requireNonNullElse(config.getSupportsStandardResponseParameters(), false))
            .xmlAttributes(config.getXmlAttributes())
            .gmlIdPrefix(Optional.ofNullable(config.getGmlIdPrefix()))
            .build();

    return Optional.of(new FeatureEncoderGml(transformationContextGml, gmlWriters));
  }
}
