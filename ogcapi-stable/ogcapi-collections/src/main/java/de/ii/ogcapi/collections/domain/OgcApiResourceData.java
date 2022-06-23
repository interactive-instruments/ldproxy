/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import de.ii.ogcapi.foundation.domain.OgcApiResource;
import org.immutables.value.Value;

/** A resource that represents data. */
@Value.Immutable
public interface OgcApiResourceData extends OgcApiResource {}
