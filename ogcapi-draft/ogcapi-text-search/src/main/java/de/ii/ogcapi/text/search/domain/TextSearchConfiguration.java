/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.text.search.domain.ImmutableTextSearchConfiguration.Builder;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTextSearchConfiguration.Builder.class)
public interface TextSearchConfiguration extends ExtensionConfiguration {

  List<String> getProperties();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  default Builder getBuilder() {
    return new ImmutableTextSearchConfiguration.Builder();
  }
}
