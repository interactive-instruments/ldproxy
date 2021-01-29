/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCollectionsConfiguration.Builder.class)
public interface CollectionsConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @JsonMerge(OptBoolean.FALSE)
    List<Link> getAdditionalLinks();

    @Override
    default Builder getBuilder() {
        return new ImmutableCollectionsConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableCollectionsConfiguration.Builder builder = new ImmutableCollectionsConfiguration.Builder().from(source)
                                                                                                           .enabled(getEnabled());

        getAdditionalLinks().forEach(link -> {
            if (!((CollectionsConfiguration) source).getAdditionalLinks()
                                                    .contains(link)) {
                builder.addAdditionalLinks(link);
            }
        });

        return builder.build();
    }
}