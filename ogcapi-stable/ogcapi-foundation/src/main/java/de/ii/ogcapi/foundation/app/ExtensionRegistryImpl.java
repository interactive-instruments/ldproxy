/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import dagger.Lazy;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@AutoBind
public class ExtensionRegistryImpl implements ExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionRegistryImpl.class);

    private final Lazy<Set<ApiExtension>> apiExtensions;

    @Inject
    ExtensionRegistryImpl(Lazy<Set<ApiExtension>> apiExtensions) {
        this.apiExtensions = apiExtensions;
    }

    @Override
    public List<ApiExtension> getExtensions() {
        return ImmutableList.copyOf(apiExtensions.get());
    }

    @Override
    public <T extends ApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
            return apiExtensions.get().stream()
                    .filter(extension -> extension!=null && extensionType.isAssignableFrom(extension.getClass()))
                    .map(extensionType::cast)
                    .collect(Collectors.toList());
    }
}
