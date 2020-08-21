package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

/**
 * A resource that has 0..n sub-resources of the same kind. GET returns a list of the sub-resources.
 * Responses may be paged. POST will add a new resource.
 *
 * Examples are {@code /collections}, {@code /collections/{collectionId}/items} or {@code /styles}.
 */
@Value.Immutable
public interface OgcApiResourceSet extends OgcApiResource {
    String getSubResourceType();
}
