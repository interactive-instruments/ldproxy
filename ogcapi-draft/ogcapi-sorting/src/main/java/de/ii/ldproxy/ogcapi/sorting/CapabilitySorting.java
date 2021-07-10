/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.sorting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.app.OgcApiEntity;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class CapabilitySorting implements ApiBuildingBlock {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilitySorting.class);
    final static List<String> VALID_TYPES = ImmutableList.of("STRING", "DATETIME", "INTEGER", "FLOAT");

    // TODO add sortables endpoint once we have agreed where to add it, for now the sortables are published
    //      in the API definition

    private final SchemaInfo schemaInfo;
    private final FeaturesCoreProviders providers;

    public CapabilitySorting(@Requires FeaturesCoreProviders providers, @Requires SchemaInfo schemaInfo) {
        this.providers = providers;
        this.schemaInfo = schemaInfo;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableSortingConfiguration.Builder().enabled(false)
                                                          .build();
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        // get the sorting configurations to process
        Map<String, SortingConfiguration> configs = apiData.getCollections()
                                                           .entrySet()
                                                           .stream()
                                                           .map(entry -> {
                                                               final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                               final SortingConfiguration config = collectionData.getExtension(SortingConfiguration.class).orElse(null);
                                                               if (Objects.isNull(config) || !config.isEnabled())
                                                                   return null;
                                                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                                                           })
                                                           .filter(Objects::nonNull)
                                                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        if (configs.isEmpty()) {
            // nothing to do
            return ValidationResult.of();
        }

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                                                                             .mode(apiValidation);

        // check that the feature provider supports sorting
        FeatureProvider2 provider = providers.getFeatureProviderOrThrow(apiData);
        if (!provider.supportsSorting())
            builder.addErrors(MessageFormat.format("Sorting is enabled, but the feature provider of the API '{0}' does not support sorting.", provider.getData().getId()));

        // no additional operational checks for now, only validation; we can stop, if no validation is requested
        if (apiValidation==ValidationResult.MODE.NONE)
            return builder.build();

        for (Map.Entry<String, SortingConfiguration> entry : configs.entrySet()) {
            List<String> sortables = entry.getValue().getSortables();

            // check that there is at least one sortable for each collection where sorting is enabled
            if (sortables.isEmpty())
                builder.addStrictErrors(MessageFormat.format("Sorting is enabled for collection ''{0}'', but no sortable property has been configured.", entry.getKey()));

            List<String> properties = schemaInfo.getPropertyNames(apiData, entry.getKey());
            FeatureSchema schema = providers.getFeatureSchema(apiData, apiData.getCollections().get(entry.getKey()));
            for (String sortable : sortables) {
                // does the collection include the sortable property?
                if (!properties.contains(sortable))
                    builder.addErrors(MessageFormat.format("The sorting configuration for collection ''{0}'' includes a sortable property ''{1}'', but the property does not exist.", entry.getKey(), sortable));

                // is it a top level, non-array property?
                schema.getProperties()
                      .stream()
                      .filter(property -> sortable.equals(property.getName()))
                      .findAny()
                      .ifPresentOrElse(property -> {
                          if (!VALID_TYPES.contains(property.getType().toString()))
                              builder.addErrors(MessageFormat.format("The sorting configuration for collection ''{0}'' includes a sortable property ''{1}'', but the property is not of a simple type. Found: ''{2}''.", entry.getKey(), sortable, property.getType().toString()));
                      }, () -> {
                          builder.addErrors(MessageFormat.format("The sorting configuration for collection ''{0}'' includes a sortable property ''{1}'', but the property is a nested property, not a top-level property.", entry.getKey(), sortable));
                      });
            }
        }

        return builder.build();
    }
}
