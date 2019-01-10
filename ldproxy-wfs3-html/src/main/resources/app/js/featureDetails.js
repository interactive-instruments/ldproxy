/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
if ($('#map').length ) {

    var osmTiles = L.tileLayer(window._ldproxy.map.url, {
        attribution: window._ldproxy.map.attribution
    });
    var geoJsonUrl;
    if (window.location.href.indexOf('f=html') > 0) {
        geoJsonUrl = window.location.href.replace('f=html', 'f=json');
    } else if (window.location.href.indexOf('?') > 0) {
        geoJsonUrl = window.location.href + "&f=json";
    } else {
        geoJsonUrl = window.location.href + "?f=json";
    }

    var map = L.map('map');

    $.getJSON(geoJsonUrl, function(data) {

        if (!data.geometry) {
            return;
        }

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

        /*var extentWidth = map.getBounds().getEast() - map.getBounds().getWest();
        var mapWidth = map.getSize().x;
        var maxAllowableOffset = extentWidth / mapWidth;
        console.log('maxAllowableOffset', maxAllowableOffset);*/


        if (data.additionalFeatures) {
            var bounds = geoJson.getBounds();
            Object.keys(data.additionalFeatures).forEach(function(ft) {
                var geoJson2 = L.geoJson(data.additionalFeatures[ft], {
                        onEachFeature: function (feature, layer) {
                                    if (feature.geometry.type != 'Point') {
                                        layer.bindPopup(feature.id);
                                    }
                                },
                                style: {
                            color: 'red',
                                fillColor: '#f03',
                                fillOpacity: 0.5
                        }
                });

                bounds.extend(geoJson2.getBounds());
                map.fitBounds(bounds, {
                        padding: [5, 5],
                        maxZoom: 16
                    });

                geoJson2.addTo(map);
            });
        }
    });
}