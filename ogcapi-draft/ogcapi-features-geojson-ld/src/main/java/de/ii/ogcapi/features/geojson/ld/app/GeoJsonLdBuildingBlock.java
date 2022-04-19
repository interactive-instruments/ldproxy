/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.app;

import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.geojson.ld.domain.ImmutableGeoJsonLdConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @title Features GeoJSON-LD (GEO_JSON_LD)
 * @en Adds support for JSON-LD by extending the [GeoJSON](geojson.md) encoding.
 * @de Das Modul "Features GeoJSON-LD" kann für jede über ldproxy bereitgestellte API mit
 * einem Feature-Provider aktiviert werden, sofern die GeoJSON-Ausgabe aktiviert ist.
 *
 * Es ergänzt die GeoJSON-Ausgabe um die folgenden Angaben:
 *
 * * Einen JSON-LD-Context, auf den aus den GeoJSON-Ausgaben der Ressourcen Features und
 * Feature verwiesen wird. Der Context kann extern liegen oder über die API bereitgestellt
 * werden. Dafür muss im ldproxy-Datenverzeichnis der Context unter dem relativen Pfad
 * `json-ld-contexts/{apiId}/{collectionId}.jsonld` liegen. Statt `{collectionId}.jsonld`
 * kann über `contextFileName` auch ein anderer Dateiname konfiguriert werden. Der Context
 * muss mindestens die folgenden Einträge enthalten:
 *   * `"@version": 1.1`
 *   * `"geojson": "https://purl.org/geojson/vocab#"`
 *   * `"FeatureCollection": "geojson:FeatureCollection"`
 *   * `"features": { "@id": "geojson:features", "@container": "@set" }`
 *   * `"Feature": "geojson:Feature"`
 *   * `"type": "geojson:type"`
 *   * `"properties": "@nest"`
 * * Zusätzlich zur Eigenschaft "type", die in GeoJSON fest mit "Feature" belegt ist, wird
 * "@type" als weitere Eigenschaft mit den in der Konfiguration angegeben Werten ergänzt.
 * * Zusätzlich zur Eigenschaft "id", wird "@id" als weitere Eigenschaft auf Basis des
 * Wertes aus "id" und dem in der Konfiguration angegeben URI-Template ergänzt. Dabei wird
 * `{{serviceUrl}}` durch die Landing-Page-URI der API, `{{collectionId}}` durch die
 * Collection-ID und `{{featureId}}` durch den Wert von "id" ersetzt.
 * @see de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration
 * @see EndpointJsonLdContext
 */
@Singleton
@AutoBind
public class GeoJsonLdBuildingBlock implements ApiBuildingBlock {

    @Inject
    public GeoJsonLdBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableGeoJsonLdConfiguration.Builder().enabled(false)
                                                            .build();
    }

}
