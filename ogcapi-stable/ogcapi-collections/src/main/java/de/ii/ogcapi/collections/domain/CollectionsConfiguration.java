/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock COLLECTIONS
 * @examplesAll <code>
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

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @default true
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @langEn Add additional links to the *Collections* resource. The value is an array of link
   *     objects. Required properties of a link are a URI (`href`), a label (`label`) and a relation
   *     (`rel`).
   * @langDe Erlaubt es, zusätzliche Links in der Ressource Feature Collections zu ergänzen. Der
   *     Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`),
   *     der anzuzeigende Text (`label`) und die Link-Relation (`rel`).
   * @default []
   */
  @JsonMerge(OptBoolean.FALSE)
  List<Link> getAdditionalLinks();

  /**
   * @langEn Controls whether each feature collection and subresource is listed as a single resource
   *     in the API definition (`false`), or whether a path parameter `collectionId` is used and
   *     each resource is specified only once in the definition (`true`). With `true` the API
   *     definition becomes simpler and shorter, but the schema is no longer collection-specific and
   *     collection-specific query parameters can no longer be specified in the API definition.
   * @langDe Steuert, ob in der API-Definition jede Feature Collection und untergeordnete Ressourcen
   *     jeweils als einzelne Ressource aufgeführt wird (`false`), oder ob ein Pfad-Parameter
   *     `collectionId` verwendet wird und jede Ressource nur einmal in der Definition spezifiziert
   *     wird (`true`). Bei `true` wird die API-Definition einfacher und kürzer, aber das Schema ist
   *     nicht mehr Collection-spezifisch und Collection-spezifische Query-Parameter können nicht
   *     mehr in der API-Definition spezifiziert werden.
   * @default false
   */
  Optional<Boolean> getCollectionIdAsParameter();

  /**
   * @langEn If in the case of `collectionIdAsParameter: true` all collections have a structurally
   *     identical schema and the same queryables, the value `true` can be used to control that in
   *     the API definition schema and queryables are determined from any collection.
   * @langDe Sofern im Fall von `collectionIdAsParameter: true` alle Collections ein strukturell
   *     identisches Schema besitzen und dieselben Queryables haben, kann mit dem Wert `true`
   *     gesteuert werden, dass in der API-Definition Schema und Queryables aus einer beliebigen
   *     Collection bestimmt werden.
   * @default false
   */
  Optional<Boolean> getCollectionDefinitionsAreIdentical();

  @Override
  default Builder getBuilder() {
    return new ImmutableCollectionsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCollectionsConfiguration.Builder builder =
        new ImmutableCollectionsConfiguration.Builder().from(source).from(this);

    List<Link> links = Lists.newArrayList(((CollectionsConfiguration) source).getAdditionalLinks());
    getAdditionalLinks()
        .forEach(
            link -> {
              if (!links.contains(link)) {
                links.add(link);
              }
            });
    builder.additionalLinks(links);

    return builder.build();
  }
}
