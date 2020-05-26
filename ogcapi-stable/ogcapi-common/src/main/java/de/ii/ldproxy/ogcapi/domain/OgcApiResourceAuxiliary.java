package de.ii.ldproxy.ogcapi.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiResource;
import org.immutables.value.Value;

/**
 * A resource that represents other types of information like styles, tiling schemes, etc.
 */
@Value.Immutable
public interface OgcApiResourceAuxiliary extends OgcApiResource {
}
