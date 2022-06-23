/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import org.immutables.value.Value;

/**
 * A resource that has 0..n sub-resources of the same kind. GET returns a list of the sub-resources.
 * Responses may be paged. POST will add a new resource.
 *
 * <p>Examples are {@code /collections}, {@code /collections/{collectionId}/items} or {@code
 * /styles}.
 */
@Value.Immutable
public interface OgcApiResourceSet extends OgcApiResource {
  String getSubResourceType();
}
