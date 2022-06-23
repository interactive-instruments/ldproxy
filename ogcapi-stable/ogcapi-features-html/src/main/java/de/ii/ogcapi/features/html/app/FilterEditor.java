/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
public interface FilterEditor {

  Set<Entry<String, String>> getFields();

  Optional<String> getBackgroundUrl();

  Optional<String> getAttribution();
}
