package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.jackson.dynamic.JacksonSubTypeIds;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class DynamicTypeIdResolverMock  implements TypeIdResolver {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(de.ii.xtraplatform.jackson.dynamic.DynamicTypeIdResolver.class);

    private JavaType mBaseType;
    private final BiMap<Class<?>, String> mapping;

    // TODO: for tests only
    public DynamicTypeIdResolverMock(JacksonSubTypeIds subTypeIds, JacksonSubTypeIds... otherJacksonSubTypeIds) {
        this.mapping = HashBiMap.create(subTypeIds.getMapping());
        Stream.of(otherJacksonSubTypeIds)
                .forEach(ids -> mapping.putAll(ids.getMapping()));
    }

    @Override
    public void init(JavaType baseType) {
        mBaseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType(null, mBaseType.getRawClass());
    }

    @Override
    public String getDescForKnownTypeIds() {
        return null;
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        if (mapping.containsKey(suggestedType)) {
            return mapping.get(suggestedType);
        }

        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        if (mapping.inverse().containsKey(id)) {
            Class<?> clazz = mapping.inverse().get(id);
            return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, clazz);
        }

        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
