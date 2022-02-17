/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import org.immutables.value.Value;

/**
 * A resource that processes some input. Pre-defined input is the parent resource. Additional input may
 * be provided via parameters and/or the request body.
 */
@Value.Immutable
public interface OgcApiResourceProcess extends OgcApiResource {
}
