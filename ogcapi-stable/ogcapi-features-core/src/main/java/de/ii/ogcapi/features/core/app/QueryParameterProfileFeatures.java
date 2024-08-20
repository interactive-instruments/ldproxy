/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import de.ii.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ProfileFeatures;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.ProfileExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title profile
 * @endpoints Features, Feature
 * @langEn This query parameter supports requesting variations in the representation of data in the
 *     same format, depending on the intended use of the data. The supported profiles depend on the
 *     provider schema of the feature collection. If a format does not support the requested
 *     profile, the best match for the requested profile is used depending on the format. The
 *     negotiated profiles are returned in links with `rel` set to `profile`. If the feature schema
 *     includes at least one property of type `FEATURE_REF` or `FEATURE_REF_ARRAY`, three profiles
 *     can be used to select the encoding of object references in the response. Supported are
 *     "rel-as-link" (default, a link with URI and a title), "rel-as-key" (the `featureId` of the
 *     referenced feature) and "rel-as-uri" (the URI of the referenced feature). Formats that only
 *     support simple values will typically not support "rel-as-link". GML only supports
 *     "rel-as-link". If the building block Codelists is enabled and the feature schema includes at
 *     least one property with a "codelist" constraint, two profiles can be used to select the
 *     representation of coded values in the response. Supported are "val-as-code" (default, the
 *     code) and "val-as-title" (the label associated with the code). HTML only supports
 *     "val-as-title", GML only supports "val-as-code".
 * @langDe Dieser Abfrageparameter unterstützt die Abfrage von Variationen in der Darstellung von
 *     Daten im gleichen Format, je nach der beabsichtigten Verwendung der Daten. Die unterstützten
 *     Profile hängen vom Provider-Schema der Feature Collection ab. Wenn ein Format das
 *     angeforderte Profil nicht unterstützt, wird je nach Format die beste Übereinstimmung für das
 *     angeforderte Profil verwendet. Die ausgehandelten Profile werden in Links zurückgegeben,
 *     wobei `rel` auf `profile` gesetzt ist. Enthält das Feature-Schema mindestens eine Eigenschaft
 *     vom Typ `FEATURE_REF` oder `FEATURE_REF_ARRAY`, können drei Profile verwendet werden, um die
 *     Kodierung der Objektreferenzen in der Antwort auszuwählen. Unterstützt werden "rel-as-link"
 *     (Standard, ein Link mit URI und einem Titel), "rel-as-key" (die `featureId` des
 *     referenzierten Features) und "rel-as-uri" (die URI des referenzierten Features). Formate, die
 *     nur einfache Werte unterstützen, unterstützen typischerweise "rel-as-link" nicht. GML
 *     unterstützt nur "rel-as-link". Wenn der Baustein Codelists aktiviert ist und das
 *     Feature-Schema mindestens eine Eigenschaft mit einer "Codelist"-Einschränkung enthält, können
 *     zwei Profile verwendet werden, um die Darstellung von codierten Werten in der Antwort
 *     auszuwählen. Unterstützt werden "val-as-code" (Standard, der Code) und "val-as-title" (die
 *     mit dem Code verbundene Bezeichnung). HTML unterstützt nur "val-as-title", GML unterstützt
 *     nur "val-as-code".
 * @default [rel-as-link,val-as-code]
 */
@Singleton
@AutoBind
public class QueryParameterProfileFeatures extends QueryParameterProfile
    implements TypedQueryParameter<List<ProfileExtension>>, ConformanceClass {

  @Inject
  public QueryParameterProfileFeatures(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId(String collectionId) {
    return "profileFeatures_" + collectionId;
  }

  @Override
  protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return (definitionPath.equals("/collections/{collectionId}/items")
            || definitionPath.equals("/collections/{collectionId}/items/{featureId}"))
        && !getProfiles(apiData).isEmpty();
  }

  @Override
  public boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
    return (definitionPath.equals("/collections/{collectionId}/items")
            || definitionPath.equals("/collections/{collectionId}/items/{featureId}"))
        && method == HttpMethods.GET
        && !getProfiles(apiData, collectionId).isEmpty();
  }

  @Override
  protected List<ProfileExtension> getProfiles(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
        .filter(profile -> profile.isEnabledForApi(apiData))
        .collect(Collectors.toList());
  }

  @Override
  protected List<ProfileExtension> getProfiles(OgcApiDataV2 apiData, String collectionId) {
    return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
        .filter(profile -> profile.isEnabledForApi(apiData, collectionId))
        .collect(Collectors.toList());
  }

  @Override
  protected List<ProfileExtension> getDefault(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
        .filter(profile -> profile.isEnabledForApi(apiData))
        .filter(ProfileFeatures::isEnabledByDefault)
        .collect(Collectors.toList());
  }

  @Override
  protected List<ProfileExtension> getDefault(OgcApiDataV2 apiData, String collectionId) {
    return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
        .filter(profile -> profile.isEnabledForApi(apiData, collectionId))
        .filter(ProfileFeatures::isEnabledByDefault)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return List.of(
        "http://www.opengis.net/spec/ogcapi-features-5/0.0/conf/profile-parameter",
        "http://www.opengis.net/spec/ogcapi-features-5/0.0/conf/profile-references");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public List<ProfileExtension> parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    if (value == null) {
      return optionalCollectionData
          .map(cd -> getDefault(api.getData(), cd.getId()))
          .orElseGet(() -> getDefault(api.getData()));
    }

    Builder<ProfileExtension> builder = ImmutableList.builder();

    List<ProfileExtension> profiles =
        optionalCollectionData
            .map(cd -> getProfiles(api.getData(), cd.getId()))
            .orElseGet(() -> getProfiles(api.getData()));

    Splitter.on(',')
        .trimResults()
        .split(value)
        .forEach(
            name ->
                builder.add(
                    profiles.stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst()
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException(
                                    String.format(
                                        "Unknown value for parameter '%s': '%s'. Known values are: [ %s ]",
                                        PROFILE,
                                        name,
                                        profiles.stream()
                                            .map(ProfileExtension::getName)
                                            .collect(Collectors.joining(", ")))))));

    return builder.build();
  }

  @Override
  public Optional<SpecificationMaturity> getSpecificationMaturity() {
    return Optional.of(SpecificationMaturity.DRAFT_OGC);
  }
}
