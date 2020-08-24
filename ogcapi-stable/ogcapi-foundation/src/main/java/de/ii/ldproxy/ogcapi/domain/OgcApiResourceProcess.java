package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

/**
 * A resource that processes some input. Pre-defined input is the parent resource. Additional input may
 * be provided via parameters and/or the request body.
 */
@Value.Immutable
public interface OgcApiResourceProcess extends OgcApiResource {
}
