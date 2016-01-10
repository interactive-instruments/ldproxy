package de.ii.ogc.wfs.proxy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xtraplatform.jackson.dynamic.DynamicTypeIdResolver;

/**
 * @author zahnen
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include=JsonTypeInfo.As.PROPERTY, property="mappingType")
@JsonTypeIdResolver(DynamicTypeIdResolver.class)
public interface TargetMapping {
    String getName();
    boolean isEnabled();
}
