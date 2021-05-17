/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;

import java.io.IOException;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;
import static de.ii.ldproxy.ogcapi.styles.domain.PathParameterStyleId.STYLE_ID_PATTERN;

public interface StyleMetadataFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/?styles/"+STYLE_ID_PATTERN+"/metadata/?$";
    }

    Object getStyleMetadataEntity(StyleMetadata metadata,
                                  OgcApiDataV2 apiData,
                                  Optional<String> collectionId,
                                  ApiRequestContext requestContext);

    StyleMetadata parse(byte[] content, boolean strict, boolean inStore);
}
