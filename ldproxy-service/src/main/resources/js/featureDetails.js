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