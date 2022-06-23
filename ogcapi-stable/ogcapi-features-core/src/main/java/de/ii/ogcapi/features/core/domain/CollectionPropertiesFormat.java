/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AutoMultiBind
public interface CollectionPropertiesFormat extends GenericFormatExtension {

  String RESOURCE_ID_PATTERN =
      String.format(
          "(?:%s)",
          Arrays.stream(CollectionPropertiesType.values())
              .map(CollectionPropertiesType::toString)
              .collect(Collectors.joining("|")));

  default String getPathPattern() {
    return "^/?collections/" + COLLECTION_ID_PATTERN + "/" + RESOURCE_ID_PATTERN + "/?$";
  }

  Object getEntity(
      JsonSchemaObject schemaCollectionProperties,
      CollectionPropertiesType type,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext);
}
