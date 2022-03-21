/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.Link;
import java.util.List;
import org.immutables.value.Value;

import java.util.Optional;

/**
 /**
 * @example <code>
 * ```yaml
 * - buildingBlock: COLLECTIONS
 *   additionalLinks:
 *   - rel: related
 *     type: text/html
 *     title: 'Weinlagen-Online website (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
 *     href: 'http://weinlagen.lwk-rlp.de/portal/weinlagen.html'
 *     hreflang: de
 *   - rel: related
 *     type: application/xml
 *     title: 'OGC Web Map Service with the data (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
 *     href: 'http://weinlagen.lwk-rlp.de/cgi-bin/mapserv?map=/data/_map/weinlagen/einzellagen_rlp.map&service=WMS&request=GetCapabilities'
 *     hreflang: de
 *   - rel: related
 *     type: application/xml
 *     title: 'OGC Web Feature Service with the data (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
 *     href: 'http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&request=getcapabilities'
 *     hreflang: de
 *   - rel: enclosure
 *     type: application/x-shape
 *     title: 'Download the data as a shapefile (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
 *     href: 'http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=lwk:Weinlagen&outputFormat=shape-zip'
 *     hreflang: de
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCollectionsConfiguration.Builder.class)
public interface CollectionsConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {

    }
  /**
   * @en Add additional links to the Collections resource. The value is an array of
   * link objects. Required properties of a link are a URI (href), a label (label)
   * and a relation (rel).
   * @de Erlaubt es, zusätzliche Links in der Ressource Feature Collections zu ergänzen.
   * Der Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens
   * die URI (href), der anzuzeigende Text (label) und die Link-Relation (rel).
   * @default []
   */
  @JsonMerge(OptBoolean.FALSE)
    List<Link> getAdditionalLinks();

    Optional<Boolean> getCollectionIdAsParameter();

    Optional<Boolean> getCollectionDefinitionsAreIdentical();

    @Override
    default Builder getBuilder() {
        return new ImmutableCollectionsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCollectionsConfiguration.Builder builder = new ImmutableCollectionsConfiguration.Builder()
        .from(source)
        .from(this);

    List<Link> links = Lists.newArrayList(((CollectionsConfiguration) source).getAdditionalLinks());
    getAdditionalLinks().forEach(link -> {
      if (!links.contains(link)) {
        links.add(link);
      }
    });
    builder.additionalLinks(links);

    return builder.build();
    }
}
