package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class QueryParameterF implements OgcApiQueryParameter {

    protected final OgcApiExtensionRegistry extensionRegistry;

    protected QueryParameterF(OgcApiExtensionRegistry extensionRegistry) {
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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET;
    }

    protected abstract Class<? extends FormatExtension> getFormatClass();
    protected Map<String, Schema> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        String key = apiData.getId()+"_*";
        if (!schemaMap.containsKey(key)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.put(key, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(key);
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        String key = apiData.getId()+"_"+collectionId;
        if (!schemaMap.containsKey(key)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData, collectionId))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.put(key, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(key);
    }
}
