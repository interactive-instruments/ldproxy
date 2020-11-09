package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class QueryParameterF implements OgcApiQueryParameter {

    protected final ExtensionRegistry extensionRegistry;

    protected QueryParameterF(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public final String getName() {
        return "f";
    }

    @Override
    public String getDescription() {
        return "Select the output format of the response. If no value is provided, " +
                "the standard HTTP rules apply, i.e., the accept header will be used to determine the format.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET;
    }

    protected abstract Class<? extends FormatExtension> getFormatClass();
    protected ConcurrentMap<Integer, ConcurrentMap<String, Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.get(apiHashCode).put("*", new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData, collectionId))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }
}
