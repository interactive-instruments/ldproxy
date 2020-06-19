/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.xtraplatform.codelists.Codelist;
import org.immutables.value.Value;

import java.util.*;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
public abstract class FeatureTransformationContextObservationProcessing implements FeatureTransformationContext {

    public abstract Map<String, Codelist> getCodelists();

    public abstract FeatureProcessChain getProcesses();
    public abstract Map<String, Object> getProcessingParameters();

    // public abstract ViewRenderer getMustacheRenderer();

    @Value.Derived
    public ObservationProcessingConfiguration getConfiguration() {
        ObservationProcessingConfiguration configuration = null;

        Optional<ObservationProcessingConfiguration> baseConfiguration = getApiData().getExtension(ObservationProcessingConfiguration.class);

        Optional<ObservationProcessingConfiguration> collectionConfiguration = Optional.ofNullable(getApiData().getCollections()
                                                                                                  .get(getCollectionId()))
                                                                          .flatMap(featureTypeConfiguration -> featureTypeConfiguration.getExtension(ObservationProcessingConfiguration.class));

        if (collectionConfiguration.isPresent()) {
            configuration = collectionConfiguration.get();
        }

        if (baseConfiguration.isPresent()) {
            if (Objects.isNull(configuration)) {
                configuration = baseConfiguration.get();
            } else {
                configuration = configuration.mergeDefaults(baseConfiguration.get());
            }
        }

        return configuration;
    }

}
