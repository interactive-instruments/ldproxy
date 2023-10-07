/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {Binder.class})
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true, builder = "new")
@BuildableMapEncodingEnabled
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import de.ii.xtraplatform.entities.domain.maptobuilder.encoding.BuildableMapEncodingEnabled;
import org.glassfish.jersey.internal.inject.Binder;
import org.immutables.value.Value;
