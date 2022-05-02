/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class PathParameterType extends ApiExtensionCache implements OgcApiPathParameter {

    protected final ExtensionRegistry extensionRegistry;
    protected final SchemaValidator schemaValidator;

    protected PathParameterType(ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
        this.extensionRegistry = extensionRegistry;
        this.schemaValidator = schemaValidator;
    }

    @Override
    public final String getName() {
        return "type";
    }

    @Override
    public String getDescription() {
        return "Select the type of the sub-resource.";
    }

    @Override
    public final boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath, () ->
            isEnabledForApi(apiData) && isApplicablePath(apiData, definitionPath));
    }

    protected abstract boolean isApplicablePath(OgcApiDataV2 apiData, String definitionPath);

    protected List<String> getValues(OgcApiDataV2 apiData, String collectionId) {
        return getValues(apiData);
    };
    protected ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            schemaMap.get(apiHashCode).put("*", new StringSchema()._enum(getValues(apiData)));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(getValues(apiData, collectionId)));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }
}
