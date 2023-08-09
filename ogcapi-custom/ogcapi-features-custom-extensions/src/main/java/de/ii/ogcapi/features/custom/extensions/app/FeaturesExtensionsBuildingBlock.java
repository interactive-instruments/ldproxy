/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.custom.extensions.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.custom.extensions.domain.ImmutableFeaturesExtensionsConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features - Custom Extensions
 * @langEn POST on `/items` and filter by geometry intersection.
 * @langDe POST für `/items` und Filterung durch Geometrie-Überschneidung.
 * @scopeEn The module *Features - Custom Extensions* adds support for the HTTP method POST on the
 *     features resource. The difference to calling with GET is that the query parameters are passed
 *     as content in the request. This may be desired for two reasons:
 *     <p><code>
 * - URLs are limited in length in HTTP implementations. Extensive filter expressions in GET
 *     calls are often too long. Using POST gets around this limitation.
 * -  When using POST, query parameters are transmitted in encrypted form when using HTTPS and are not logged in request
 *     logs. This may be desirable for security or privacy reasons.
 *     </code>
 *     <p>Note that this capability has been deprecated. Instead, use an Ad-hoc Query from
 *     [Features - Search](#features_-_search.html).
 *     <p>The module further adds support for the following query parameter:
 *     <p><code>
 * - `intersects`: if the parameter is specified, the features are
 *     additionally selected by the geometry specified as value and only features whose primary
 *     geometry intersects with the specified geometry are returned. The geometry can be either a
 *     WKT geometry or a URL for a GeoJSON object with a geometry. In case of a FeatureCollection
 *     the first geometry is used.
 *     </code>
 * @scopeDe Das Modul *Features - Custom Extensions* ergänzt die Unterstützung der HTTP-Methode POST
 *     auf der Features-Ressource. Der Unterschied zum Aufruf mit GET ist, dass die Query-Parameter
 *     als Content im Aufruf übergeben werden. Dies kann aus zwei Gründen gewünscht sein:
 *     <p><code>
 * - URLs sind in HTTP-Implementierungen in der Länge beschränkt. Umfangreiche
 *     Filterausdrücke in GET-Aufrufen sind oft zu lang. Die Verwendung von POST umgeht diese
 *     Einschränkung.
 * - Bei der Verwendung von POST werden die Query-Parameter bei der Verwendung
 *     von HTTPS verschlüsselt übertragen und werden nicht in Request-Logs protokolliert. Dies kann
 *     aus Sicherheits- oder Datenschutzgründen erwünscht sein.
 *     </code>
 *     <p>Bitte beachten Sie, dass diese Funktionalität veraltet ist. Benutzen Sie stattdessen eine
 *     Ad-hoc-Query aus [Features - Search](#features_-_search.html).
 *     <p>Das Modul ergänzt weiterhin die Unterstützung für den folgenden Query-Parameter:
 *     <p><code>
 * - `intersects`: Ist der Parameter angegeben, werden die Features
 *     zusätzlich nach der als Wert angegeben Geometrie selektiert und es werden nur Features
 *     zurückgeliefert, deren primäre Geometrie sich mit der angegebenen Geometrie schneidet. Als
 *     Geometrie kann entweder eine WKT-Geometrie angegeben werden oder eine URL für ein
 *     GeoJSON-Objekt mit einer Geometrie. Im Fall einer FeatureCollection wird die erste Geometrie
 *     verwendet.
 *     </code>
 * @ref:cfg {@link de.ii.ogcapi.features.custom.extensions.domain.FeaturesExtensionsConfiguration}
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.features.custom.extensions.domain.ImmutableFeaturesExtensionsConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.features.custom.extensions.infra.EndpointPostOnItems}
 * @ref:pathParameters {@link de.ii.ogcapi.features.core.domain.PathParameterCollectionIdFeatures}
 * @ref:queryParameters {@link de.ii.ogcapi.features.custom.extensions.app.QueryParameterIntersects}
 */
@Singleton
@AutoBind
public class FeaturesExtensionsBuildingBlock implements ApiBuildingBlock {

  @Inject
  public FeaturesExtensionsBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).postOnItems(false).intersectsParameter(false).build();
  }
}
