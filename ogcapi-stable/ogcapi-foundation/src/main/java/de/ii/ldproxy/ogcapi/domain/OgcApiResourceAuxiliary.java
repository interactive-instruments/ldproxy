/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

/**
 * A resource that represents other types of information like styles, tiling schemes, etc.
 */
@Value.Immutable
public interface OgcApiResourceAuxiliary extends OgcApiResource {
}
