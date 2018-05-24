/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
var osmTiles = L.tileLayer(window._ldproxy.map.url, {
    attribution: window._ldproxy.map.attribution
});
var geoJsonUrl = '?f=json&';
if (window.location.href.indexOf('?') > 0) {
    geoJsonUrl += window.location.href.slice(window.location.href.indexOf('?') + 1);
}
var map = L.map('map');

$.getJSON(geoJsonUrl, function(data) {
    var geoJson = L.geoJson(data, {
        onEachFeature: function (feature, layer) {
            if (feature.geometry.type != 'Point') {
                layer.bindPopup(feature.id);
            }
        },
        pointToLayer: function (feature, latlng) {
            return L.marker(latlng, {title: feature.id}).addTo(map);
        }
    });
    map.fitBounds(geoJson.getBounds(), {
        padding: [50, 50],
        maxZoom: 16
    });
    osmTiles.addTo(map);
    geoJson.addTo(map);
});