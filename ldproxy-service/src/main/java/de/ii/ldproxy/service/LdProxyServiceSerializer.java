/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.ii.xsf.configstore.api.rest.GenericResourceSerializer;
import de.ii.xsf.core.api.JsonViews;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;

/**
 * @author zahnen
 */
public class LdProxyServiceSerializer extends GenericResourceSerializer<LdProxyService> {
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyServiceSerializer.class);

    private final ObjectMapper updateJsonMapper;

    public LdProxyServiceSerializer(ObjectMapper jsonMapper) {
        super(jsonMapper);
        updateJsonMapper = new ObjectMapper();
        updateJsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        updateJsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        updateJsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public String serializeAdd(LdProxyService resource) throws IOException {
        return jsonMapper
                .writerWithView(JsonViews.FullView.class)
                .writeValueAsString(resource);
    }

    @Override
    public String serializeUpdate(LdProxyService resource) throws IOException {
        return jsonMapper
                .writerWithView(JsonViews.ConfigurationView.class)
                .without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .writeValueAsString(resource);
    }
}

