/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
if ($('#map').length ) {
    var basemapTiles = L.tileLayer(window._ldproxy.map.url, {
        attribution: window._ldproxy.map.attribution
    });
    const tilesUrl = document.getElementById('tilesUrl').innerText.replace("{tileMatrix}","{z}").replace("{tileRow}","{y}").replace("{tileCol}","{x}")
    var vectorTiles = L.vectorGrid.protobuf(tilesUrl);
    var map = L.map('map');
    map.fitBounds(window._ldproxy.map.bounds, {
    });
    basemapTiles.addTo(map);
    vectorTiles.addTo(map);
}