package de.ii.ldproxy.ogcapi.domain;

import com.google.common.base.Splitter;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A resource is any path/URI supported by the API. Through path parameters any instance may represent
 * multiple resources of the API.
 */
public interface OgcApiResource {
    /*
    String getApiEntrypoint();
    String getSubPath();
    @Value.Derived
    @Value.Auxiliary
    default String getPath() {
        return "/"+getApiEntrypoint()+getSubPath();
    }
     */
    String getPath();

    List<OgcApiPathParameter> getPathParameters();
    Map<String, OgcApiOperation> getOperations();

    @Value.Derived
    @Value.Auxiliary
    default boolean isSubCollectionWithExplicitId() {
        return getPathParameters().stream().anyMatch(param -> param.getName().equals("collectionId") && param.getExplodeInOpenApi());
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<String> getCollectionId() {
        return isSubCollectionWithExplicitId()? Optional.ofNullable(Splitter.on("/").limit(3).omitEmptyStrings().splitToList(getPath()).get(1)) :Optional.empty();
    }

    @Value.Derived
    @Value.Auxiliary
    default String getPathPattern() {
        String path = getPath();
        for (OgcApiPathParameter param : getPathParameters()) {
            path = path.replace("{"+param.getName()+"}", param.getPattern());
        }
        return "^(?:"+path+")/?$";
    }

    @Value.Derived
    @Value.Auxiliary
    default Pattern getPathPatternCompiled() {
        return Pattern.compile(getPathPattern());
    }

    /*
    @Value.Derived
    @Value.Auxiliary
    default String getSubPathPattern() {
        String path = getSubPath();
        for (OgcApiPathParameter param : getPathParameters()) {
            path = path.replace("{"+param.getName()+"}", param.getPattern());
        }
        return "^(?:"+path+")/?$";
    }

    @Value.Derived
    @Value.Auxiliary
    default Pattern getSubPathPatternCompiled() {
        return Pattern.compile(getSubPathPattern());
    }
     */

}
