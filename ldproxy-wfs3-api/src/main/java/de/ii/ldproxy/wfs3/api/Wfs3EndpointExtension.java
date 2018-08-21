package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
public interface Wfs3EndpointExtension extends Wfs3Extension {

    String getPath();

    default List<String> getMethods() {
        return ImmutableList.of();
    }

    default boolean matches(String path, String method) {
        return Objects.nonNull(path) && path.startsWith(getPath()) && (getMethods().isEmpty() || getMethods().contains(method));
    }
}
