/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.json;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.SchemaGenerator;
import io.swagger.v3.oas.models.media.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class SchemaGeneratorImpl implements SchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaGeneratorImpl.class);

    private final ConcurrentMap<String, Schema> schemaMap;

    @Inject
    public SchemaGeneratorImpl() {
        this.schemaMap = new ConcurrentHashMap<>();
    }

    // TODO complete and make robust
    public Schema getSchema(Class clazz) {

        switch (clazz.getSimpleName()) {
            case "String":
                return new StringSchema();
            case "EpsgCrs":
            case "URI":
                return new StringSchema().format("uri-reference");
            case "Locale":
                return new StringSchema().pattern("^[a-zA-Z]{1,8}(?:\\-[a-zA-Z0-9]{1,8})?$");
            case "int":
            case "Integer":
            case "long":
            case "Long":
                return new IntegerSchema();
            case "BigDecimal":
            case "double":
            case "Double":
            case "float":
            case "Float":
                return new NumberSchema();
            case "boolean":
            case "Boolean":
                return new BooleanSchema();
            case "Link":
                return new ObjectSchema().addProperties("href", new StringSchema().format("uri-reference"))
                                         .addProperties("rel", new StringSchema())
                                         .addProperties("type", new StringSchema())
                                         .addProperties("title", new StringSchema())
                                         .addRequiredItem("href");
        }

        if (!schemaMap.containsKey(clazz.getName()))
            schemaMap.put(clazz.getName(), generateSchema(clazz));

        return schemaMap.get(clazz.getName());
    }

    private Schema generateSchema(Class clazz) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Generating schema for class '" + clazz.getName() + "'.");
        }

        if (clazz.isEnum()) {
            Object[] enums = clazz.getEnumConstants();
            return new StringSchema()._enum(Arrays.asList(enums).stream().map(val -> val.toString()).collect(Collectors.toList()));
        }

        if (clazz.isArray()) {
            return new ArraySchema().items(getSchema(clazz.getComponentType()));
        }

        Schema schema = new ObjectSchema();
        while (clazz!=null && clazz!=java.lang.Object.class) {
            for (Method m : clazz.getDeclaredMethods()) {
                boolean ignore = false;
                for (Annotation a : m.getAnnotations()) {
                    if (Objects.equals(a.annotationType(), com.fasterxml.jackson.annotation.JsonIgnore.class)) {
                        ignore = true;
                        break;
                    }
                    if (Objects.equals(a.annotationType(), com.fasterxml.jackson.annotation.JsonAnyGetter.class)) {
                        ignore = true;
                        break;
                    }
                }
                if (ignore)
                    continue;
                if (m.getParameterCount()>0)
                    continue;
                int mod = m.getModifiers();
                if (!Modifier.isPublic(mod))
                    continue;

                String name = m.getName();
                if (name.startsWith("get"))
                    name = name.substring(3);
                else if (name.startsWith("is"))
                    name = name.substring(2);
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

                Type type = m.getGenericReturnType();
                try {
                    // special treatment for some classes
                    if (type.getTypeName().equals("java.util.OptionalDouble"))
                        type = this.getClass().getDeclaredMethod("getOptionalDouble", new Class[]{}).getGenericReturnType();
                    else if (type.getTypeName().equals("java.util.OptionalInt"))
                        type = this.getClass().getDeclaredMethod("getOptionalInteger", new Class[]{}).getGenericReturnType();
                } catch (NoSuchMethodException e) {
                    // skip property
                    continue;
                }
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType)type;
                    Type rType = pType.getRawType();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Raw type: " + rType + " - Type args: " + pType.getActualTypeArguments());
                    }
                    schema.addProperties(name, generateSchema(pType));
                } else {
                    type = m.getReturnType();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Type: " + type);
                    }
                    boolean required = true;
                    for (Annotation a : m.getAnnotations()) {
                        if (Objects.equals(a.annotationType(), javax.annotation.Nullable.class)) {
                            required = false;
                            break;
                        }
                    }
                    if (required)
                        schema.addRequiredItem(name);
                    if (type instanceof Class<?>)
                        schema.addProperties(name, getSchema((Class<?>)type));
                    else
                        schema.addProperties(name, new Schema());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return schema;
    }

    private static Optional<Double> getOptionalDouble() { return Optional.of(0.0); }
    private static Optional<Integer> getOptionalInteger() { return Optional.of(0); }

    private Schema generateSchema(ParameterizedType pType) {
        Type rType = pType.getRawType();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Raw type: " + rType + " - Type args: " + pType.getActualTypeArguments());
        }
        if (rType.getTypeName().equals("java.util.List") || rType.getTypeName().equals("java.util.List")) {
            Type aType = pType.getActualTypeArguments()[0];
            if (aType instanceof Class<?>)
                return new ArraySchema().items(getSchema((Class<?>)aType));
            else if (aType instanceof ParameterizedType)
                return new ArraySchema().items(generateSchema((ParameterizedType)aType));

        } else if (rType.getTypeName().equals("java.util.Optional")) {
            Type aType = pType.getActualTypeArguments()[0];
            if (aType instanceof Class<?>)
                return getSchema((Class<?>)aType);
            else if (aType instanceof ParameterizedType)
                return generateSchema((ParameterizedType)aType);
        } else if (rType.getTypeName().equals("java.util.Map")) {
            Type a1Type = pType.getActualTypeArguments()[0];
            Type a2Type = pType.getActualTypeArguments()[1];
            if (a2Type instanceof Class<?>)
                return new ObjectSchema().additionalProperties(getSchema((Class<?>) a2Type));
            else if (a2Type instanceof ParameterizedType)
                return new ObjectSchema().additionalProperties(generateSchema((ParameterizedType) a2Type));
        }

        return new Schema();
    }
}
