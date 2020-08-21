/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.ii.xtraplatform.dropwizard.api.JacksonSubTypeIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class DynamicTypeIdResolverMock  implements TypeIdResolver {

    public static HandlerInstantiator handlerInstantiator() {
        return new HandlerInstantiator() {
            @Override
            public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
                return null;
            }

            @Override
            public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
                return null;
            }

            @Override
            public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
                return null;
            }

            @Override
            public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
                return null;
            }

            @Override
            public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
                return new DynamicTypeIdResolverMock(new JacksonSubTypeIdsGeoJson() /*TODO , new Wfs3GenericMappingSubTypeIds(), new MicrodataMappingSubTypeIds(), new JsonLdMappingSubTypeIds()*/);
            }
        };
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTypeIdResolverMock.class);

    private JavaType mBaseType;
    private final BiMap<Class<?>, String> mapping;

    // TODO: for tests only
    DynamicTypeIdResolverMock(JacksonSubTypeIds subTypeIds, JacksonSubTypeIds... otherJacksonSubTypeIds) {
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
