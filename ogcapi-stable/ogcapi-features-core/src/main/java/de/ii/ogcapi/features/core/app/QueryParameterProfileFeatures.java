/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.Profile;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title profile
 * @endpoints Features, Feature
 * @langEn Select the encoding of object references in the response. Supported are "rel-as-link"
 *     (default, a link with URI and a title), "rel-as-key" (the `featureId` of the referenced
 *     feature) and "rel-as-uri" (the URI of the referenced feature). If an encoding does not
 *     support the requested profile, the best match for the requested profile is used depending on
 *     the encoding. Feature encodings that typically only support simple values will typically not
 *     support "rel-as-link". The negotiated profile is returned in a link with `rel` set to
 *     `profile`.
 * @langDe Wählen Sie die Kodierung der Objektreferenzen in der Antwort. Unterstützt werden
 *     "rel-as-link" (Standardwert, ein Link mit URI und einem Titel), "rel-as-key" (die `featureId`
 *     des referenzierten Features) und "rel-as-uri" (die URI des referenzierten Features). Wenn ein
 *     Format das angeforderte Profil nicht unterstützt, wird je nach Format die beste
 *     Übereinstimmung für das angeforderte Profil verwendet. Formate, die nur einfache Werte
 *     unterstützen, unterstützen in der Regel "rel-as-link" nicht. Das ausgehandelte Profil wird in
 *     einem Link zurückgegeben, wobei `rel` auf `profile` gesetzt ist.
 * @default rel-as-link
 */
@Singleton
@AutoBind
public class QueryParameterProfileFeatures extends QueryParameterProfile
    implements TypedQueryParameter<Profile> {

  private final FeaturesCoreProviders providers;

  @Inject
  public QueryParameterProfileFeatures(
      ExtensionRegistry extensionRegistry,
      SchemaValidator schemaValidator,
      FeaturesCoreProviders providers) {
    super(extensionRegistry, schemaValidator);
    this.providers = providers;
  }

  @Override
  public String getId() {
    return "profileFeatures";
  }

  @Override
  protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return (definitionPath.equals("/collections/{collectionId}/items")
            || definitionPath.equals("/collections/{collectionId}/items/{featureId}"))
        && usesFeatureRef(apiData);
  }

  @Override
  protected List<String> getProfiles(OgcApiDataV2 apiData) {
    return Arrays.stream(Profile.values())
        .map(Profile::getProfileName)
        .collect(Collectors.toList());
  }

  @Override
  protected String getDefault(OgcApiDataV2 apiData) {
    return Profile.AS_LINK.getProfileName();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public Profile parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    if (value == null) {
      return Profile.getDefault();
    }

    switch (value) {
      case "rel-as-key":
        return Profile.AS_KEY;
      case "rel-as-link":
        return Profile.AS_LINK;
      case "rel-as-uri":
        return Profile.AS_URI;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Unknown value for parameter '%s': '%s'. Known values are: [ %s ]",
                PROFILE,
                value,
                Arrays.stream(Profile.values())
                    .map(Profile::getProfileName)
                    .collect(Collectors.joining(", "))));
    }
  }

  private boolean usesFeatureRef(OgcApiDataV2 apiData) {
    return apiData.getCollections().values().stream()
        .anyMatch(
            collectionData ->
                providers
                    .getFeatureSchema(apiData, collectionData)
                    .map(
                        schema ->
                            schema.getAllNestedProperties().stream()
                                // FIXME
                                .anyMatch(SchemaBase::isFeatureRef))
                    .orElse(false));
  }

  @Override
  public Optional<SpecificationMaturity> getSpecificationMaturity() {
    return Optional.of(SpecificationMaturity.DRAFT_OGC);
  }
}
