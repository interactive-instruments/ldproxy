/*
    tiles.mustache

    <script src="https://cdn.jsdelivr.net/gh/openlayers/openlayers.github.io@master/en/v6.5.0/build/ol.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/proj4js/2.6.0/proj4.js"></script>
*/

/*
    header.mustache

    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/openlayers/openlayers.github.io@master/en/v6.5.0/css/ol.css" type="text/css"/>
    <style>
    .map {
        width: 100%;
        height: 600px;
        position: relative;
        background: #f8f4f0;
    }

    .info {
        z-index: 1;
        opacity: 0;
        position: absolute;
        bottom: 0;
        left: 0;
        margin: 0;
        background: rgba(0, 0, 0, 0.5);
        color: white;
        border: 0;
        transition: opacity 100ms ease-in;
    }
    </style>
*/

/*
    footer.mustache

    <script>
        const template = "{{{tilesUrl}}}";

        proj4.defs('EPSG:25832', '+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs');
        proj4.defs('EPSG:3395', '+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs');
        ol.proj.proj4.register(proj4);
        ol.proj.get('EPSG:25832').setExtent([-46133.17, 5048875.268576, 1206211.101424, 6301219.54]);
        ol.proj.get('EPSG:3395').setExtent([-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244]);
        var baseLayer = new ol.layer.Tile({
                            source: new ol.source.XYZ({
                                attributions: window._ldproxy.ol_map.attribution,
                                url: window._ldproxy.ol_map.url
                            })
                        });
        var map = new ol.Map({
            target: 'map-container',
                    view: new ol.View({
                        projection: 'EPSG:3857',
                    }),
                    layers: [ baseLayer ]
        });

        var info = document.getElementById('info');
        map.on('pointermove', showInfo);

        var xyzTemplate = document.getElementById('xyzTemplate');
        var howToUse = document.getElementById('howToUse');

    {{#tileCollections}}
        var tilesUrl{{tileMatrixSet}} = template.replace("{tileMatrixSetId}","{{tileMatrixSet}}").replace("{tileMatrix}","{z}").replace("{tileRow}","{y}").replace("{tileCol}","{x}")

        var maxLevel{{tileMatrixSet}} = {{maxLevel}};
        var resolutions{{tileMatrixSet}} = {{resolutions}};

        var sizes{{tileMatrixSet}} = {{sizes}};

        var tileGrid{{tileMatrixSet}} = new ol.tilegrid.TileGrid({
            extent: {{extent}},
            resolutions: resolutions{{tileMatrixSet}},
            sizes: sizes{{tileMatrixSet}}
        });

    var tilesUrl{{tileMatrixSet}} = template.replace("{tileMatrixSetId}","{{tileMatrixSet}}").replace("{tileMatrix}","{z}").replace("{tileRow}","{y}").replace("{tileCol}","{x}");

        {{#tileJsonLink}}
    var tileJsonUrl{{tileMatrixSet}} = "{{.}}?f=tilejson".replace("{tileMatrixSetId}","{{tileMatrixSet}}");
        {{/tileJsonLink}}

    var view{{tileMatrixSet}} = new ol.View({
            projection: '{{projection}}'
            });

    var layer{{tileMatrixSet}} = new ol.layer.VectorTile({
                declutter: true,
                title: "Vector Tiles",
                source: new ol.source.VectorTile({
                format: new ol.format.MVT(),
                maxZoom: maxLevel{{tileMatrixSet}},
                projection: '{{projection}}',
                tileGrid: tileGrid{{tileMatrixSet}},
                url: tilesUrl{{tileMatrixSet}}
                })
            });
        const extent{{tileMatrixSet}} = ol.proj.transformExtent(window._ldproxy.ol_map.extent, 'EPSG:4326', '{{projection}}');
        const center{{tileMatrixSet}} = ol.proj.fromLonLat([ {{defaultCenterLon}}, {{defaultCenterLat}} ], '{{projection}}');
    const zoom{{tileMatrixSet}} = {{defaultZoomLevel}};

    {{/tileCollections}}
    {{^tileCollections}}
        var tilesUrl = template.replace("{tileMatrixSetId}","WebMercatorQuad").replace("{tileMatrix}","{z}").replace("{tileRow}","{y}").replace("{tileCol}","{x}")
        var map = new ol.Map({
            target: 'map-container',
                    view: new ol.View({
                    }),
                    layers: [
                        new ol.layer.Tile({
                            source: new ol.source.XYZ({
                                attributions: window._ldproxy.ol_map.attribution,
                                url: window._ldproxy.ol_map.url
                            })
                        }),
                        new ol.layer.VectorTile({
                            declutter: true,
                            title: "Vector Tiles",
                            source: new ol.source.VectorTile({
                                format: new ol.format.MVT(),
                                url: tilesUrl
                            })
                        })
                    ]
        });
        const extent = ol.proj.transformExtent(window._ldproxy.ol_map.extent, 'EPSG:4326', 'EPSG:3857');
        map.getView().fit(extent, map.getSize());

        map.on('pointermove', showInfo);
        var info = document.getElementById('info');
        function showInfo(event) {
            var features = map.getFeaturesAtPixel(event.pixel);
            if (!features) {
                info.innerText = '';
                info.style.opacity = 0;
                return;
            }
            if (features[0]) {
                var id = features[0].getId();
                var properties = features[0].getProperties();
                if (id) {
                    properties.id = id;
                }
                info.innerText = JSON.stringify(properties, null, 2);
                info.style.opacity = 1;
            }
        }
    {{/tileCollections}}

    setTilingScheme();

    function setTilingScheme() {
    var val = document.getElementById("tilingScheme").value;
    {{#tileCollections}}
    if (val === "{{tileMatrixSet}}") {
    map.setView(view{{tileMatrixSet}});
    map.getLayers().setAt(1, layer{{tileMatrixSet}});
    map.getView().setCenter(center{{tileMatrixSet}})
    map.getView().setZoom(zoom{{tileMatrixSet}});
            tilejson.href = tileJsonUrl{{tileMatrixSet}};
            xyzTemplate.innerText = tilesUrl{{tileMatrixSet}};
    }
    {{/tileCollections}}
        if (val === "WebMercatorQuad") {
            howToUse.style.display = "block";
        } else {
            howToUse.style.display = "none";
        };
    info.innerText = '';
    info.style.opacity = 0;
    }

        function showInfo(event) {
            var features = map.getFeaturesAtPixel(event.pixel);
            if (!features) {
                info.innerText = '';
                info.style.opacity = 0;
                return;
            }
            if (features[0]) {
                var id = features[0].getId();
                var properties = features[0].getProperties();
                if (id) {
                    properties.id = id;
                }
                info.innerText = JSON.stringify(properties, null, 2);
                info.style.opacity = 1;
            }
        }
    </script>
*/
