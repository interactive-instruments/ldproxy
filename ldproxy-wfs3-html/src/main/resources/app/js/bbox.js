/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
if ($('#map').length ) {

    var osmTiles = L.tileLayer(window._ldproxy.map.url, {
        attribution: window._ldproxy.map.attribution
    });

    var map = L.map('map');

    map.fitBounds(window._ldproxy.map.bounds, {
        padding: [30, 30],
        maxZoom: 16
    });

    var boundingBox = L.rectangle(window._ldproxy.map.bounds, {color: "#ff0000", weight: 1});
    osmTiles.addTo(map);
    boundingBox.addTo(map);
}