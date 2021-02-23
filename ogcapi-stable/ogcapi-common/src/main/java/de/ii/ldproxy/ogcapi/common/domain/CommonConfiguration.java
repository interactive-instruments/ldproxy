/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.Link;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCommonConfiguration.Builder.class)
public interface CommonConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {

  }

  @JsonMerge(OptBoolean.FALSE)
  List<Link> getAdditionalLinks();

  @Override
  default Builder getBuilder() {
    return new ImmutableCommonConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCommonConfiguration.Builder builder = new ImmutableCommonConfiguration.Builder()
        .from(source)
        .from(this);

    List<Link> links = Lists.newArrayList(((CommonConfiguration) source).getAdditionalLinks());
    getAdditionalLinks().forEach(link -> {
      if (!links.contains(link)) {
        links.add(link);
      }
    });
    builder.additionalLinks(links);

    return builder.build();
  }
}