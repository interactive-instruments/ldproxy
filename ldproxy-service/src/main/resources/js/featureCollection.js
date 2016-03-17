/*
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var osmTiles = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
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
                L.marker(layer.getBounds().getCenter(), {title: feature.id}).addTo(map);
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
