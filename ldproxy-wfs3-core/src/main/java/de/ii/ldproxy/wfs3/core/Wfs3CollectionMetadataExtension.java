package de.ii.ldproxy.wfs3.core;

import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Extension;

/**
 * @author zahnen
 */
public interface Wfs3CollectionMetadataExtension extends Wfs3Extension {

    Wfs3Collection process(Wfs3Collection collection, URICustomizer uriCustomizer, boolean isNested);
}
