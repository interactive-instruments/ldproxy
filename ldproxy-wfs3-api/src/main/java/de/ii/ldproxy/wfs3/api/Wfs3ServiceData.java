/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerServiceData;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.feature.query.api.TargetMapping.BASE_TYPE;

/**
 * @author zahnen
 */

@Value.Immutable
@Value.Modifiable
@Value.Style(
        deepImmutablesDetection = true
)
@JsonDeserialize(as = ModifiableWfs3ServiceData.class)
public abstract class Wfs3ServiceData extends FeatureTransformerServiceData<FeatureTypeConfigurationWfs3> {

    public static final String DEFAULT_CRS_URI = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final EpsgCrs DEFAULT_CRS = new EpsgCrs(4326, true);
    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceData.class);

    @Value.Default
    @Override
    public String getLabel() {
        return getId();
    }

    @Value.Default
    @Override
    public long getCreatedAt() {
        return Instant.now().toEpochMilli();
    }

    @Value.Default
    @Override
    public long getLastModified() {
        return Instant.now().toEpochMilli();
    }

    @JsonMerge
    @Override
    public abstract Map<String, FeatureTypeConfigurationWfs3> getFeatureTypes();

    public abstract List<EpsgCrs> getAdditionalCrs();

    public abstract Optional<Wfs3ServiceMetadata> getMetadata();

    public boolean isFeatureTypeEnabled(final String featureType) {
        return getFeatureProvider().isFeatureTypeEnabled(featureType);
    }

    public Map<String, String> getFilterableFieldsForFeatureType(String featureType) {
        return getFilterableFieldsForFeatureType(featureType, false);
    }

    public Map<String, String> getFilterableFieldsForFeatureType(String featureType, boolean withoutSpatialAndTemporal) {
        return getFeatureProvider().getMappings()
                                   .get(featureType)
                                   .findMappings(BASE_TYPE)
                                   .entrySet()
                                   .stream()
                                   .filter(isFilterable(withoutSpatialAndTemporal))
                                   .collect(Collectors.toMap(getParameterName(), mapping -> propertyPathsToCql(mapping.getKey()
                                                                                                   .substring(mapping.getKey()
                                                                                                                     .lastIndexOf(":") + 1))));
    }

    //TODO: move to html module
    public Map<String, String> getHtmlNamesForFeatureType(String featureType) {
        return getFeatureProvider().getMappings()
                                   .get(featureType)
                                   .findMappings("text/html")
                                   .entrySet()
                                   .stream()
                                   .filter(mapping -> mapping.getValue()
                                                             .getName() != null && mapping.getValue()
                                                                                          .isEnabled())
                                   .map(mapping -> new AbstractMap.SimpleImmutableEntry<>(propertyPathsToCql(mapping.getKey()
                                                                                                                    .substring(mapping.getKey()
                                                                                                                                      .lastIndexOf(":") + 1)), mapping.getValue()
                                                                                                                                                                      .getName()))
                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2) -> s));
    }

    private String propertyPathsToCql(String propertyPath) {
        return propertyPathsToShort(propertyPath).replace('/', '.');
    }

    private String propertyPathsToShort(String propertyPath) {

        return propertyPath.replaceAll("(?:(?:(^| |\\()/)|(/))(?:\\[\\w+=\\w+\\])?(?:\\w+\\()*(\\w+)(?:\\)(?:,| |\\)))*", "$1$2$3");
    }

    private Function<Map.Entry<String, TargetMapping>, String> getParameterName() {
        return mapping -> mapping.getValue()
                                 .isSpatial() ? "bbox"
                : ((Wfs3GenericMapping) mapping.getValue()).isTemporal() ? "time"
                : mapping.getValue()
                         .getName()
                         .replaceAll("\\[\\w+\\]", "")
                         .toLowerCase();
    }

    private Predicate<Map.Entry<String, TargetMapping>> isFilterable(boolean withoutSpatialAndTemporal) {
        return mapping -> ((Wfs3GenericMapping) mapping.getValue()).isFilterable() &&
                (mapping.getValue()
                        .getName() != null || mapping.getValue()
                                                     .isSpatial()) &&
                mapping.getValue()
                       .isEnabled() &&
                (!withoutSpatialAndTemporal || (!mapping.getValue()
                                                        .isSpatial() && !((Wfs3GenericMapping) mapping.getValue()).isTemporal()));
    }

    static class ModifiableOptionalPropertyDeserializer extends StdDeserializer<Wfs3ServiceData> {

        public ModifiableOptionalPropertyDeserializer() {
            super(Wfs3ServiceData.class);
        }

        @Override
        public Wfs3ServiceData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            LOGGER.debug("DESERIALIZE WFS3");

            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            Wfs3ServiceData deserializedUser = null;

            DeserializationConfig config = ctxt.getConfig();
            JavaType javaType = TypeFactory.defaultInstance()
                                           .constructSimpleType(ModifiableWfs3ServiceData.class, new JavaType[0]);
            JsonDeserializer<Object> defaultDeserializer = BeanDeserializerFactory.instance.buildBeanDeserializer(ctxt, javaType, config.introspect(javaType));

            if (defaultDeserializer instanceof ResolvableDeserializer) {
                ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
            }

            JsonParser treeParser = oc.treeAsTokens(node);
            config.initialize(treeParser);

            if (treeParser.getCurrentToken() == null) {
                treeParser.nextToken();
            }

            deserializedUser = (Wfs3ServiceData) defaultDeserializer.deserialize(treeParser, ctxt);

            return deserializedUser;
        }
    }
}
