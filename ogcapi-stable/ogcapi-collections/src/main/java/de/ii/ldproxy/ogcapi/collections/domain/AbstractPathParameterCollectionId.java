/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractPathParameterCollectionId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPathParameterCollectionId.class);

    public static final String COLLECTION_ID_PATTERN = "[\\w\\-]+";

    protected final Map<Integer, Boolean> apiExplodeMap;

    protected final Map<Integer,List<String>> apiCollectionMap;

    public AbstractPathParameterCollectionId() {
        this.apiCollectionMap = new HashMap<>();
        this.apiExplodeMap = new HashMap<>();
    }

    @Override
    public String getPattern() {
        return COLLECTION_ID_PATTERN;
    }

    @Override
    public boolean getExplodeInOpenApi(OgcApiDataV2 apiData) {
        if (!apiExplodeMap.containsKey(apiData.hashCode())) {
            apiExplodeMap.put(apiData.hashCode(), !apiData.getExtension(CollectionsConfiguration.class)
                                                          .get()
                                                          .getCollectionIdAsParameter()
                                                          .orElse(false));
        }

        return apiExplodeMap.get(apiData.hashCode());
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode())) {
            apiCollectionMap.put(apiData.hashCode(), apiData.getCollections().keySet().stream()
                                                            .filter(collectionId -> apiData.isCollectionEnabled(collectionId))
                                                            .collect(Collectors.toUnmodifiableList()));
        }

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getName() {
        return "collectionId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a feature collection.";
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }
}
