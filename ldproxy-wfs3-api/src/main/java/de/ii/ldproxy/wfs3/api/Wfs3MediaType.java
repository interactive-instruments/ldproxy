package de.ii.ldproxy.wfs3.api;

import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class Wfs3MediaType {

    public abstract MediaType main();

    @Value.Default
    public MediaType metadata() {
        return main();
    }

    @Value.Default
    public String label() {
        return main().getSubtype().toUpperCase();
    }

    @Value.Default
    public String metadataLabel() {
        return metadata().getSubtype().toUpperCase();
    }

    @Value.Derived
    public String parameter() {
        return main().getSubtype().contains("+") ? main().getSubtype().substring(main().getSubtype().lastIndexOf("+")+1)  : main().getSubtype();
    }

    @Value.Default
    public int qs() {
        return 1000;
    }

    public boolean matches(MediaType mediaType) {
        return main().isCompatible(mediaType) || metadata().isCompatible(mediaType);
    }

}
