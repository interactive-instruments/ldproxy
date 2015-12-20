package de.ii.xtraplatform.jackson.dynamic;

import java.util.Map;

/**
 * @author zahnen
 */
public interface JacksonSubTypeIds {
    Map<Class<?>, String> getMapping();
}
